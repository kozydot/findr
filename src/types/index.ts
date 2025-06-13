export interface PriceHistoryEntry {
  date: string;
  price: number;
}

export interface Retailer {
  retailerId: string;
  name: string;
  logo: string;
  currentPrice: number;
  priceHistory: PriceHistoryEntry[];
  inStock: boolean;
  freeShipping: boolean;
  shippingCost: number;
  productUrl: string;
}

export interface Specification {
  name: string;
  value: string;
}

export interface Product {
  id: string;
  name: string;
  description: string;
  imageUrl: string;
  rating: number;
  reviews: number;
  retailers: Retailer[];
  specifications?: Specification[];
  productInformation?: { [key: string]: string };
  price?: string;
  currency?: string;
}

export interface Alert {
  productId: string;
  productName: string;
  targetPrice: number;
  currentPrice: number;
  imageUrl: string;
}
