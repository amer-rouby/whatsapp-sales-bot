import { BACKEND_PREFIX } from "@/shared/auth/constants";
import { apiClient, ApiError } from "@/shared/lib/api-client";
import type { Product, ProductImage, ProductInput } from "../types";

const BASE = "/api/products";

/** Changes from client components (through proxy.ts). Reads live in products-queries.ts. */
export const productsApi = {
  create: (input: ProductInput) => apiClient.post<Product>(BASE, input),
  update: (id: string, input: ProductInput) => apiClient.put<Product>(`${BASE}/${id}`, input),
  remove: (id: string) => apiClient.delete<void>(`${BASE}/${id}`),
  removeImage: (id: string, imageId: string) => apiClient.delete<void>(`${BASE}/${id}/images/${imageId}`),
  reorderImages: (id: string, imageIds: string[]) => apiClient.put<Product>(`${BASE}/${id}/images/order`, { imageIds }),
  uploadImage,
};

/**
 * Uploads one edited photo. XMLHttpRequest instead of fetch because fetch can't report upload progress.
 * @param onProgress 0..1
 */
function uploadImage(id: string, photo: Blob, onProgress?: (fraction: number) => void): Promise<ProductImage> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", `${BACKEND_PREFIX}${BASE}/${id}/images`);
    xhr.setRequestHeader("Accept", "application/json");
    xhr.responseType = "json";
    xhr.upload.onprogress = (e) => e.lengthComputable && onProgress?.(e.loaded / e.total);
    xhr.onload = () => {
      if (xhr.status === 401) {
        window.location.assign("/logout");
        return;
      }
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(xhr.response as ProductImage);
      } else {
        const problem = (xhr.response ?? {}) as { detail?: string; title?: string };
        reject(new ApiError({ ...problem, status: xhr.status }));
      }
    };
    xhr.onerror = () => reject(new TypeError("Network error"));
    const form = new FormData();
    form.append("file", photo, "photo.jpg");
    xhr.send(form);
  });
}
