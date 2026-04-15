import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";

import {
  applySessionResolution,
  clearSessionCookie,
  resolveSessionForRequest,
} from "./auth";

const apiGatewayUrl =
  process.env.API_GATEWAY_URL ?? process.env.NEXT_PUBLIC_API_GATEWAY_URL ?? "http://localhost:8085";

async function parseBody(response: Response) {
  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get("content-type") ?? "";

  if (contentType.includes("application/json")) {
    return response.json();
  }

  return response.text();
}

function unauthorizedResponse(clearSession = false) {
  const response = NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  if (clearSession) {
    clearSessionCookie(response);
  }

  return response;
}

export async function proxyGatewayRequest(
  request: NextRequest,
  path: string,
  init?: {
    method?: string;
    body?: unknown;
  },
) {
  const resolution = await resolveSessionForRequest(request);

  if (!resolution.session) {
    return unauthorizedResponse(resolution.clearSession);
  }

  const headers = new Headers({
    Accept: "application/json",
    Authorization: `Bearer ${resolution.session.accessToken}`,
  });

  let body: string | undefined;

  if (init?.body !== undefined) {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(init.body);
  }

  const upstream = await fetch(`${apiGatewayUrl}${path}`, {
    method: init?.method ?? request.method,
    headers,
    body,
    cache: "no-store",
  });

  const payload = await parseBody(upstream);
  const response =
    upstream.status === 204
      ? new NextResponse(null, { status: 204 })
      : typeof payload === "string"
        ? new NextResponse(payload, {
            status: upstream.status,
            headers: {
              "content-type": upstream.headers.get("content-type") ?? "text/plain; charset=utf-8",
            },
          })
        : NextResponse.json(payload, { status: upstream.status });

  if (upstream.status === 401) {
    clearSessionCookie(response);
  } else {
    applySessionResolution(response, resolution);
  }

  return response;
}
