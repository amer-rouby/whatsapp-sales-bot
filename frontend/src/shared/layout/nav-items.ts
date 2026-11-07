/** Sidebar entries. Adding a screen = one line here + a route folder. */
export interface NavItem {
  href: string;
  label: string;
}

export const NAV_ITEMS: NavItem[] = [
  { href: "/conversations", label: "المحادثات" },
  { href: "/orders", label: "الأوردرات" },
  { href: "/products", label: "المنتجات" },
  { href: "/simulator", label: "جرّب البوت" },
];
