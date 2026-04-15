import { redirect } from "next/navigation";

import { readServerSession } from "../lib/auth";

export default async function HomePage() {
  const session = await readServerSession();

  redirect(session ? "/app" : "/sign-in");
}
