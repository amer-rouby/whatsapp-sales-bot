import { Bot, LayoutDashboard, MessagesSquare, Package, ShoppingBag, type LucideIcon } from "lucide-react";
import type { Dictionary } from "@/shared/i18n/dictionaries/ar";

/** Sidebar entries. Adding a screen = one line here + a route folder + its label in the dictionaries. */
export interface NavItem {
  href: string;
  label: keyof Dictionary["nav"];
  icon: LucideIcon;
}

export const NAV_ITEMS: NavItem[] = [
  { href: "/", label: "overview", icon: LayoutDashboard },
  { href: "/orders", label: "orders", icon: ShoppingBag },
  { href: "/products", label: "products", icon: Package },
  { href: "/conversations", label: "conversations", icon: MessagesSquare },
  { href: "/simulator", label: "simulator", icon: Bot },
];

/** The nav item a path belongs to ("/products/123/edit" → products). */
export function activeNavItem(pathname: string): NavItem | undefined {
  return NAV_ITEMS.find((item) =>
    item.href === "/" ? pathname === "/" : pathname === item.href || pathname.startsWith(`${item.href}/`),
  );
}
