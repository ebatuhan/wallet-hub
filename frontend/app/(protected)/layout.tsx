import type { ReactNode } from "react";
import { redirect } from "next/navigation";

import { ProtectedShell } from "../../components/protected-shell";
import { readServerSession } from "../../lib/auth";

export default async function ProtectedLayout({ children }: { children: ReactNode }) {
  const session = await readServerSession();

  if (!session) {
    redirect("/sign-in");
  }

  return <ProtectedShell user={session.user}>{children}</ProtectedShell>;
}
