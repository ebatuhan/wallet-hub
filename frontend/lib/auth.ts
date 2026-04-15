import { createHash, randomBytes } from "node:crypto";

import { cookies } from "next/headers";
import type { NextRequest, NextResponse } from "next/server";
import { decodeJwt } from "jose";

import type { SessionUser } from "./types";

type TokenResponse = {
  access_token: string;
  expires_in: number;
  refresh_token?: string;
  id_token?: string;
};

export type SessionPayload = {
  accessToken: string;
  refreshToken?: string;
  idToken?: string;
  expiresAt: number;
  user: SessionUser;
};

export type SessionResolution = {
  session: SessionPayload | null;
  refreshedSession?: SessionPayload;
  clearSession: boolean;
};

const ACCESS_COOKIE_NAME = "wallet_hub_access_token";
const REFRESH_COOKIE_NAME = "wallet_hub_refresh_token";
const PKCE_COOKIE_NAME = "wallet_hub_pkce";
const STATE_COOKIE_NAME = "wallet_hub_state";
const SESSION_LIFETIME_SECONDS = 60 * 60 * 24 * 7;
const REFRESH_BUFFER_SECONDS = 60;

const keycloakIssuer =
  process.env.KEYCLOAK_ISSUER ?? "http://localhost:8080/realms/keycloak-realm";
const keycloakClientId = process.env.KEYCLOAK_CLIENT_ID ?? "wallet-hub-web";

function cookieOptions(maxAge = SESSION_LIFETIME_SECONDS) {
  return {
    httpOnly: true,
    sameSite: "lax" as const,
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge,
  };
}

function buildCallbackUrl(origin: string) {
  return `${origin}/api/auth/callback`;
}

function tokenEndpoint() {
  return `${keycloakIssuer}/protocol/openid-connect/token`;
}

export function getKeycloakClientId() {
  return keycloakClientId;
}

export function createAuthRequest(origin: string, mode: "login" | "register") {
  const verifier = randomBytes(32).toString("base64url");
  const challenge = createHash("sha256").update(verifier).digest("base64url");
  const state = randomBytes(24).toString("base64url");
  const url = new URL(`${keycloakIssuer}/protocol/openid-connect/auth`);

  url.searchParams.set("client_id", keycloakClientId);
  url.searchParams.set("redirect_uri", buildCallbackUrl(origin));
  url.searchParams.set("response_type", "code");
  url.searchParams.set("scope", "openid profile email offline_access");
  url.searchParams.set("code_challenge", challenge);
  url.searchParams.set("code_challenge_method", "S256");
  url.searchParams.set("state", state);

  if (mode === "register") {
    url.searchParams.set("prompt", "create");
  }

  return {
    verifier,
    state,
    url: url.toString(),
  };
}

async function exchangeGrant(body: URLSearchParams) {
  const response = await fetch(tokenEndpoint(), {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body,
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(await response.text());
  }

  return (await response.json()) as TokenResponse;
}

function readUser(accessToken: string): SessionUser {
  try {
    const claims = decodeJwt(accessToken);

    return {
      sub: String(claims.sub ?? ""),
      name: String(claims.name ?? claims.preferred_username ?? claims.email ?? "Wallet Hub user"),
      email: typeof claims.email === "string" ? claims.email : undefined,
      username: typeof claims.preferred_username === "string" ? claims.preferred_username : undefined,
    };
  } catch {
    return {
      sub: "",
      name: "Wallet Hub user",
    };
  }
}

function buildSession(tokenResponse: TokenResponse): SessionPayload {
  const claims = decodeJwt(tokenResponse.access_token);

  return {
    accessToken: tokenResponse.access_token,
    refreshToken: tokenResponse.refresh_token,
    expiresAt:
      typeof claims.exp === "number"
        ? claims.exp
        : Math.floor(Date.now() / 1000) + Math.max(tokenResponse.expires_in - 10, 0),
    user: readUser(tokenResponse.access_token),
  };
}

function buildSessionFromCookies(accessToken: string, refreshToken?: string) {
  try {
    const claims = decodeJwt(accessToken);
    const expiresAt = typeof claims.exp === "number" ? claims.exp : 0;

    return {
      accessToken,
      refreshToken,
      expiresAt,
      user: readUser(accessToken),
    } satisfies SessionPayload;
  } catch {
    return null;
  }
}

async function refreshSession(session: SessionPayload) {
  if (!session.refreshToken) {
    return null;
  }

  const params = new URLSearchParams({
    grant_type: "refresh_token",
    client_id: keycloakClientId,
    refresh_token: session.refreshToken,
  });

  try {
    const tokenResponse = await exchangeGrant(params);
    return buildSession(tokenResponse);
  } catch {
    return null;
  }
}

export async function createSessionFromCode(origin: string, code: string, verifier: string) {
  const params = new URLSearchParams({
    grant_type: "authorization_code",
    client_id: keycloakClientId,
    code,
    redirect_uri: buildCallbackUrl(origin),
    code_verifier: verifier,
  });

  const tokenResponse = await exchangeGrant(params);
  return buildSession(tokenResponse);
}

export async function readServerSession() {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get(ACCESS_COOKIE_NAME)?.value;
  const refreshToken = cookieStore.get(REFRESH_COOKIE_NAME)?.value;

  if (!accessToken) {
    return null;
  }

  const session = buildSessionFromCookies(accessToken, refreshToken);

  if (!session) {
    return null;
  }

  const now = Math.floor(Date.now() / 1000);

  if (session.expiresAt > now + REFRESH_BUFFER_SECONDS) {
    return session;
  }

  return refreshSession(session);
}

export async function readSessionFromRequest(request: NextRequest) {
  const accessToken = request.cookies.get(ACCESS_COOKIE_NAME)?.value;
  const refreshToken = request.cookies.get(REFRESH_COOKIE_NAME)?.value;

  if (!accessToken) {
    return null;
  }

  return buildSessionFromCookies(accessToken, refreshToken);
}

export async function resolveSessionForRequest(request: NextRequest): Promise<SessionResolution> {
  const accessToken = request.cookies.get(ACCESS_COOKIE_NAME)?.value;
  const refreshToken = request.cookies.get(REFRESH_COOKIE_NAME)?.value;

  if (!accessToken) {
    return {
      session: null,
      clearSession: false,
    };
  }

  const session = buildSessionFromCookies(accessToken, refreshToken);

  if (!session) {
    return {
      session: null,
      clearSession: true,
    };
  }

  const now = Math.floor(Date.now() / 1000);

  if (session.expiresAt > now + REFRESH_BUFFER_SECONDS) {
    return {
      session,
      clearSession: false,
    };
  }

  const refreshed = await refreshSession(session);

  if (!refreshed) {
    return {
      session: null,
      clearSession: true,
    };
  }

  return {
    session: refreshed,
    refreshedSession: refreshed,
    clearSession: false,
  };
}

export function setSessionCookies(response: NextResponse, session: SessionPayload) {
  response.cookies.set(ACCESS_COOKIE_NAME, session.accessToken, cookieOptions());

  if (session.refreshToken) {
    response.cookies.set(REFRESH_COOKIE_NAME, session.refreshToken, cookieOptions());
  } else {
    response.cookies.set(REFRESH_COOKIE_NAME, "", cookieOptions(0));
  }
}

export function clearSessionCookies(response: NextResponse) {
  response.cookies.set(ACCESS_COOKIE_NAME, "", cookieOptions(0));
  response.cookies.set(REFRESH_COOKIE_NAME, "", cookieOptions(0));
}

export function clearSessionCookie(response: NextResponse) {
  clearSessionCookies(response);
}

export function applySessionResolution(response: NextResponse, resolution: SessionResolution) {
  if (resolution.clearSession) {
    clearSessionCookies(response);
    return;
  }

  if (resolution.refreshedSession) {
    setSessionCookies(response, resolution.refreshedSession);
  }
}

export function setAuthFlowCookies(response: NextResponse, verifier: string, state: string) {
  response.cookies.set(PKCE_COOKIE_NAME, verifier, cookieOptions(60 * 10));
  response.cookies.set(STATE_COOKIE_NAME, state, cookieOptions(60 * 10));
}

export function clearAuthFlowCookies(response: NextResponse) {
  response.cookies.set(PKCE_COOKIE_NAME, "", cookieOptions(0));
  response.cookies.set(STATE_COOKIE_NAME, "", cookieOptions(0));
}

export function readAuthFlowState(request: NextRequest) {
  return {
    verifier: request.cookies.get(PKCE_COOKIE_NAME)?.value,
    state: request.cookies.get(STATE_COOKIE_NAME)?.value,
  };
}

export function buildLogoutUrl(origin: string) {
  const url = new URL(`${keycloakIssuer}/protocol/openid-connect/logout`);
  url.searchParams.set("post_logout_redirect_uri", `${origin}/sign-in`);
  url.searchParams.set("client_id", keycloakClientId);

  return url.toString();
}
