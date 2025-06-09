import { Product } from '../types';

export const mockSuggestions = [
  "iPhone 13 Pro",
  "Samsung Galaxy S22",
  "AirPods Pro",
  "PlayStation 5",
  "Nintendo Switch",
  "Apple Watch Series 7",
  "MacBook Air M2",
  "Dyson V12",
  "Sony WH-1000XM4",
  "iPad Air",
  "LG OLED TV",
  "Nespresso Machine",
  "Kitchen Aid Mixer",
  "Samsung Washing Machine",
  "Ninja Air Fryer"
];

export const mockFeaturedProducts: Product[] = [
  {
    id: "prod-001",
    name: "Apple iPhone 13 Pro Max (256GB) - Sierra Blue",
    description: "The iPhone 13 Pro Max features a 6.7-inch Super Retina XDR display with ProMotion, an advanced camera system for improved low-light photography, and exceptional battery life.",
    imageUrl: "https://images.pexels.com/photos/5750001/pexels-photo-5750001.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1",
    rating: 4.8,
    reviews: 3245,
    retailers: [
      {
        id: "ret-001",
        name: "Amazon.ae",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/Amazon_logo.svg/320px-Amazon_logo.svg.png",
        currentPrice: 4349,
        priceHistory: [
          { date: "2023-10-15", price: 4349 },
          { date: "2023-10-08", price: 4399 },
          { date: "2023-10-01", price: 4499 },
          { date: "2023-09-24", price: 4499 },
          { date: "2023-09-17", price: 4599 }
        ],
        inStock: true,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.amazon.ae/"
      },
      {
        id: "ret-002",
        name: "Noon",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e9/Noon_logo.svg/320px-Noon_logo.svg.png",
        currentPrice: 4399,
        priceHistory: [
          { date: "2023-10-15", price: 4399 },
          { date: "2023-10-08", price: 4399 },
          { date: "2023-10-01", price: 4449 },
          { date: "2023-09-24", price: 4449 },
          { date: "2023-09-17", price: 4499 }
        ],
        inStock: true,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.noon.com/"
      },
      {
        id: "ret-003",
        name: "Lulu",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cc/Lulu_Hypermarket_logo.svg/320px-Lulu_Hypermarket_logo.svg.png",
        currentPrice: 4449,
        priceHistory: [
          { date: "2023-10-15", price: 4449 },
          { date: "2023-10-08", price: 4499 },
          { date: "2023-10-01", price: 4499 },
          { date: "2023-09-24", price: 4599 },
          { date: "2023-09-17", price: 4599 }
        ],
        inStock: true,
        freeShipping: false,
        shippingCost: 15,
        productUrl: "https://www.luluhypermarket.com/"
      }
    ],
    specifications: [
      { name: "Display", value: "6.7-inch Super Retina XDR with ProMotion" },
      { name: "Processor", value: "A15 Bionic chip" },
      { name: "Storage", value: "256GB" },
      { name: "Camera", value: "Pro 12MP camera system" },
      { name: "Battery", value: "Up to 28 hours video playback" },
      { name: "OS", value: "iOS 15" }
    ]
  },
  {
    id: "prod-002",
    name: "Samsung Galaxy S22 Ultra (256GB) - Phantom Black",
    description: "The Samsung Galaxy S22 Ultra features a 6.8-inch Dynamic AMOLED 2X display, a sophisticated camera system for stunning photography, and S Pen support.",
    imageUrl: "https://images.pexels.com/photos/13018390/pexels-photo-13018390.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1",
    rating: 4.7,
    reviews: 2876,
    retailers: [
      {
        id: "ret-001",
        name: "Amazon.ae",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/Amazon_logo.svg/320px-Amazon_logo.svg.png",
        currentPrice: 3899,
        priceHistory: [
          { date: "2023-10-15", price: 3899 },
          { date: "2023-10-08", price: 3899 },
          { date: "2023-10-01", price: 3999 },
          { date: "2023-09-24", price: 4099 },
          { date: "2023-09-17", price: 4099 }
        ],
        inStock: true,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.amazon.ae/"
      },
      {
        id: "ret-002",
        name: "Noon",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e9/Noon_logo.svg/320px-Noon_logo.svg.png",
        currentPrice: 3849,
        priceHistory: [
          { date: "2023-10-15", price: 3849 },
          { date: "2023-10-08", price: 3949 },
          { date: "2023-10-01", price: 3949 },
          { date: "2023-09-24", price: 4049 },
          { date: "2023-09-17", price: 4049 }
        ],
        inStock: true,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.noon.com/"
      },
      {
        id: "ret-003",
        name: "Lulu",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cc/Lulu_Hypermarket_logo.svg/320px-Lulu_Hypermarket_logo.svg.png",
        currentPrice: 3999,
        priceHistory: [
          { date: "2023-10-15", price: 3999 },
          { date: "2023-10-08", price: 3999 },
          { date: "2023-10-01", price: 4099 },
          { date: "2023-09-24", price: 4099 },
          { date: "2023-09-17", price: 4199 }
        ],
        inStock: true,
        freeShipping: false,
        shippingCost: 15,
        productUrl: "https://www.luluhypermarket.com/"
      }
    ],
    specifications: [
      { name: "Display", value: "6.8-inch Dynamic AMOLED 2X" },
      { name: "Processor", value: "Snapdragon 8 Gen 1" },
      { name: "Storage", value: "256GB" },
      { name: "Camera", value: "108MP camera system" },
      { name: "Battery", value: "5000mAh" },
      { name: "OS", value: "Android 12" }
    ]
  },
  {
    id: "prod-003",
    name: "Sony PlayStation 5 Console (Disc Version)",
    description: "The PlayStation 5 features lightning-fast loading with an ultra-high speed SSD, deeper immersion with support for haptic feedback, adaptive triggers, and 3D Audio.",
    imageUrl: "https://images.pexels.com/photos/12718477/pexels-photo-12718477.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1",
    rating: 4.9,
    reviews: 5420,
    retailers: [
      {
        id: "ret-001",
        name: "Amazon.ae",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/Amazon_logo.svg/320px-Amazon_logo.svg.png",
        currentPrice: 2099,
        priceHistory: [
          { date: "2023-10-15", price: 2099 },
          { date: "2023-10-08", price: 2199 },
          { date: "2023-10-01", price: 2199 },
          { date: "2023-09-24", price: 2299 },
          { date: "2023-09-17", price: 2299 }
        ],
        inStock: false,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.amazon.ae/"
      },
      {
        id: "ret-002",
        name: "Noon",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e9/Noon_logo.svg/320px-Noon_logo.svg.png",
        currentPrice: 2049,
        priceHistory: [
          { date: "2023-10-15", price: 2049 },
          { date: "2023-10-08", price: 2149 },
          { date: "2023-10-01", price: 2149 },
          { date: "2023-09-24", price: 2249 },
          { date: "2023-09-17", price: 2249 }
        ],
        inStock: true,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.noon.com/"
      },
      {
        id: "ret-003",
        name: "Lulu",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cc/Lulu_Hypermarket_logo.svg/320px-Lulu_Hypermarket_logo.svg.png",
        currentPrice: 2149,
        priceHistory: [
          { date: "2023-10-15", price: 2149 },
          { date: "2023-10-08", price: 2149 },
          { date: "2023-10-01", price: 2249 },
          { date: "2023-09-24", price: 2249 },
          { date: "2023-09-17", price: 2349 }
        ],
        inStock: true,
        freeShipping: false,
        shippingCost: 20,
        productUrl: "https://www.luluhypermarket.com/"
      }
    ],
    specifications: [
      { name: "CPU", value: "8-core AMD Zen 2" },
      { name: "GPU", value: "10.28 TFLOPS, AMD RDNA 2" },
      { name: "Storage", value: "825GB SSD" },
      { name: "Resolution", value: "Up to 8K" },
      { name: "Disc Drive", value: "4K UHD Blu-ray" },
      { name: "Controller", value: "DualSense wireless controller" }
    ]
  }
];

// Extend the mock data for search results and product details
export const mockProductDetails: Product[] = [
  ...mockFeaturedProducts,
  {
    id: "prod-004",
    name: "Apple AirPods Pro (2nd Generation)",
    description: "AirPods Pro feature Active Noise Cancellation, Transparency mode, spatial audio with dynamic head tracking, and a customizable fit.",
    imageUrl: "https://images.pexels.com/photos/3780681/pexels-photo-3780681.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1",
    rating: 4.7,
    reviews: 2943,
    retailers: [
      {
        id: "ret-001",
        name: "Amazon.ae",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/Amazon_logo.svg/320px-Amazon_logo.svg.png",
        currentPrice: 949,
        priceHistory: [
          { date: "2023-10-15", price: 949 },
          { date: "2023-10-08", price: 949 },
          { date: "2023-10-01", price: 999 },
          { date: "2023-09-24", price: 999 },
          { date: "2023-09-17", price: 1099 }
        ],
        inStock: true,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.amazon.ae/"
      },
      {
        id: "ret-002",
        name: "Noon",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e9/Noon_logo.svg/320px-Noon_logo.svg.png",
        currentPrice: 929,
        priceHistory: [
          { date: "2023-10-15", price: 929 },
          { date: "2023-10-08", price: 979 },
          { date: "2023-10-01", price: 979 },
          { date: "2023-09-24", price: 1049 },
          { date: "2023-09-17", price: 1049 }
        ],
        inStock: true,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.noon.com/"
      },
      {
        id: "ret-003",
        name: "Lulu",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cc/Lulu_Hypermarket_logo.svg/320px-Lulu_Hypermarket_logo.svg.png",
        currentPrice: 969,
        priceHistory: [
          { date: "2023-10-15", price: 969 },
          { date: "2023-10-08", price: 999 },
          { date: "2023-10-01", price: 999 },
          { date: "2023-09-24", price: 1079 },
          { date: "2023-09-17", price: 1079 }
        ],
        inStock: true,
        freeShipping: false,
        shippingCost: 10,
        productUrl: "https://www.luluhypermarket.com/"
      }
    ],
    specifications: [
      { name: "Chip", value: "H2 headphone chip" },
      { name: "Noise Control", value: "Active Noise Cancellation and Transparency mode" },
      { name: "Spatial Audio", value: "Yes, with dynamic head tracking" },
      { name: "Sweat and Water Resistant", value: "Yes (IPX4)" },
      { name: "Battery Life", value: "Up to 6 hours of listening time" },
      { name: "Charging Case", value: "MagSafe Charging Case with speaker and lanyard loop" }
    ]
  },
  {
    id: "prod-005",
    name: "Samsung 65-Inch QN90B Neo QLED 4K Smart TV",
    description: "Experience brilliantly intense 4K color powered by AI with Samsung Neo QLED. The ultra-precise Quantum Mini LEDs create incredible contrast with deep blacks and bright whites.",
    imageUrl: "https://images.pexels.com/photos/4009402/pexels-photo-4009402.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1",
    rating: 4.6,
    reviews: 1253,
    retailers: [
      {
        id: "ret-001",
        name: "Amazon.ae",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/Amazon_logo.svg/320px-Amazon_logo.svg.png",
        currentPrice: 4999,
        priceHistory: [
          { date: "2023-10-15", price: 4999 },
          { date: "2023-10-08", price: 5299 },
          { date: "2023-10-01", price: 5299 },
          { date: "2023-09-24", price: 5499 },
          { date: "2023-09-17", price: 5499 }
        ],
        inStock: true,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.amazon.ae/"
      },
      {
        id: "ret-002",
        name: "Noon",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e9/Noon_logo.svg/320px-Noon_logo.svg.png",
        currentPrice: 5099,
        priceHistory: [
          { date: "2023-10-15", price: 5099 },
          { date: "2023-10-08", price: 5099 },
          { date: "2023-10-01", price: 5399 },
          { date: "2023-09-24", price: 5399 },
          { date: "2023-09-17", price: 5599 }
        ],
        inStock: true,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.noon.com/"
      },
      {
        id: "ret-003",
        name: "Lulu",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cc/Lulu_Hypermarket_logo.svg/320px-Lulu_Hypermarket_logo.svg.png",
        currentPrice: 5299,
        priceHistory: [
          { date: "2023-10-15", price: 5299 },
          { date: "2023-10-08", price: 5299 },
          { date: "2023-10-01", price: 5499 },
          { date: "2023-09-24", price: 5699 },
          { date: "2023-09-17", price: 5699 }
        ],
        inStock: false,
        freeShipping: false,
        shippingCost: 50,
        productUrl: "https://www.luluhypermarket.com/"
      }
    ],
    specifications: [
      { name: "Display Type", value: "Neo QLED" },
      { name: "Resolution", value: "4K UHD 3840 x 2160" },
      { name: "Screen Size", value: "65 inches" },
      { name: "Refresh Rate", value: "120Hz" },
      { name: "HDR", value: "Quantum HDR 32x" },
      { name: "Smart TV", value: "Yes, Tizen OS" }
    ]
  },
  {
    id: "prod-006",
    name: "Dyson V15 Detect Absolute Cordless Vacuum Cleaner",
    description: "The Dyson V15 Detect Absolute vacuum reveals dust you didn't know was there, with a laser that detects particles invisible to the eye.",
    imageUrl: "https://images.pexels.com/photos/4108715/pexels-photo-4108715.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1",
    rating: 4.8,
    reviews: 897,
    retailers: [
      {
        id: "ret-001",
        name: "Amazon.ae",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/Amazon_logo.svg/320px-Amazon_logo.svg.png",
        currentPrice: 2899,
        priceHistory: [
          { date: "2023-10-15", price: 2899 },
          { date: "2023-10-08", price: 2999 },
          { date: "2023-10-01", price: 2999 },
          { date: "2023-09-24", price: 3199 },
          { date: "2023-09-17", price: 3199 }
        ],
        inStock: true,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.amazon.ae/"
      },
      {
        id: "ret-002",
        name: "Noon",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e9/Noon_logo.svg/320px-Noon_logo.svg.png",
        currentPrice: 2949,
        priceHistory: [
          { date: "2023-10-15", price: 2949 },
          { date: "2023-10-08", price: 2949 },
          { date: "2023-10-01", price: 3099 },
          { date: "2023-09-24", price: 3099 },
          { date: "2023-09-17", price: 3299 }
        ],
        inStock: true,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.noon.com/"
      },
      {
        id: "ret-003",
        name: "Lulu",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cc/Lulu_Hypermarket_logo.svg/320px-Lulu_Hypermarket_logo.svg.png",
        currentPrice: 2999,
        priceHistory: [
          { date: "2023-10-15", price: 2999 },
          { date: "2023-10-08", price: 3099 },
          { date: "2023-10-01", price: 3099 },
          { date: "2023-09-24", price: 3299 },
          { date: "2023-09-17", price: 3299 }
        ],
        inStock: true,
        freeShipping: false,
        shippingCost: 25,
        productUrl: "https://www.luluhypermarket.com/"
      }
    ],
    specifications: [
      { name: "Suction Power", value: "230 AW" },
      { name: "Run Time", value: "Up to 60 minutes" },
      { name: "Weight", value: "3.1 kg" },
      { name: "Bin Volume", value: "0.76 L" },
      { name: "Filtration", value: "HEPA filtration" },
      { name: "Features", value: "Green laser dust detection, acoustic dust sensing" }
    ]
  },
  {
    id: "prod-007",
    name: "Ninja DZ090 Foodi 6-in-1 DualZone Air Fryer",
    description: "The Ninja Foodi DualZone Air Fryer lets you cook two foods, two ways, and finish at the same time with its unique dual-zone technology.",
    imageUrl: "https://images.pexels.com/photos/5824516/pexels-photo-5824516.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1",
    rating: 4.7,
    reviews: 1432,
    retailers: [
      {
        id: "ret-001",
        name: "Amazon.ae",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/Amazon_logo.svg/320px-Amazon_logo.svg.png",
        currentPrice: 749,
        priceHistory: [
          { date: "2023-10-15", price: 749 },
          { date: "2023-10-08", price: 799 },
          { date: "2023-10-01", price: 799 },
          { date: "2023-09-24", price: 899 },
          { date: "2023-09-17", price: 899 }
        ],
        inStock: true,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.amazon.ae/"
      },
      {
        id: "ret-002",
        name: "Noon",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e9/Noon_logo.svg/320px-Noon_logo.svg.png",
        currentPrice: 729,
        priceHistory: [
          { date: "2023-10-15", price: 729 },
          { date: "2023-10-08", price: 779 },
          { date: "2023-10-01", price: 779 },
          { date: "2023-09-24", price: 849 },
          { date: "2023-09-17", price: 849 }
        ],
        inStock: true,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.noon.com/"
      },
      {
        id: "ret-003",
        name: "Lulu",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cc/Lulu_Hypermarket_logo.svg/320px-Lulu_Hypermarket_logo.svg.png",
        currentPrice: 779,
        priceHistory: [
          { date: "2023-10-15", price: 779 },
          { date: "2023-10-08", price: 779 },
          { date: "2023-10-01", price: 829 },
          { date: "2023-09-24", price: 829 },
          { date: "2023-09-17", price: 899 }
        ],
        inStock: true,
        freeShipping: false,
        shippingCost: 15,
        productUrl: "https://www.luluhypermarket.com/"
      }
    ],
    specifications: [
      { name: "Capacity", value: "7.6L (2 x 3.8L baskets)" },
      { name: "Power", value: "2400W" },
      { name: "Functions", value: "Air Fry, Max Crisp, Roast, Bake, Reheat, Dehydrate" },
      { name: "Temperature Range", value: "40°C - 240°C" },
      { name: "Dishwasher Safe", value: "Yes, removable parts" },
      { name: "Sync Feature", value: "Yes, finish cooking at the same time" }
    ]
  },
  {
    id: "prod-008",
    name: "Sony WH-1000XM5 Wireless Noise Cancelling Headphones",
    description: "Industry-leading noise cancellation, exceptional sound quality, and crystal-clear call quality. The Sony WH-1000XM5 headphones rewrite the rules for distraction-free listening.",
    imageUrl: "https://images.pexels.com/photos/3394651/pexels-photo-3394651.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1",
    rating: 4.9,
    reviews: 2187,
    retailers: [
      {
        id: "ret-001",
        name: "Amazon.ae",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/Amazon_logo.svg/320px-Amazon_logo.svg.png",
        currentPrice: 1399,
        priceHistory: [
          { date: "2023-10-15", price: 1399 },
          { date: "2023-10-08", price: 1399 },
          { date: "2023-10-01", price: 1499 },
          { date: "2023-09-24", price: 1499 },
          { date: "2023-09-17", price: 1599 }
        ],
        inStock: true,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.amazon.ae/"
      },
      {
        id: "ret-002",
        name: "Noon",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e9/Noon_logo.svg/320px-Noon_logo.svg.png",
        currentPrice: 1379,
        priceHistory: [
          { date: "2023-10-15", price: 1379 },
          { date: "2023-10-08", price: 1449 },
          { date: "2023-10-01", price: 1449 },
          { date: "2023-09-24", price: 1549 },
          { date: "2023-09-17", price: 1549 }
        ],
        inStock: true,
        freeShipping: true,
        shippingCost: 0,
        productUrl: "https://www.noon.com/"
      },
      {
        id: "ret-003",
        name: "Lulu",
        logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cc/Lulu_Hypermarket_logo.svg/320px-Lulu_Hypermarket_logo.svg.png",
        currentPrice: 1449,
        priceHistory: [
          { date: "2023-10-15", price: 1449 },
          { date: "2023-10-08", price: 1449 },
          { date: "2023-10-01", price: 1549 },
          { date: "2023-09-24", price: 1549 },
          { date: "2023-09-17", price: 1649 }
        ],
        inStock: true,
        freeShipping: false,
        shippingCost: 15,
        productUrl: "https://www.luluhypermarket.com/"
      }
    ],
    specifications: [
      { name: "Driver Unit", value: "30mm, dome type" },
      { name: "Frequency Response", value: "4Hz-40,000Hz" },
      { name: "Battery Life", value: "Up to 30 hours with NC on" },
      { name: "Charging Time", value: "3.5 hours (full charge)" },
      { name: "Quick Charge", value: "3 hours playback from 3 min charge" },
      { name: "Bluetooth", value: "Bluetooth 5.2" }
    ]
  }
];