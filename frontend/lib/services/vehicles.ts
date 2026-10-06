import { request } from './api';
import type { VehicleRequest, VehicleResponse } from '../types';

export async function getVehicles(): Promise<VehicleResponse[]> {
  return request<VehicleResponse[]>('/me/vehicles');
}

export async function getVehicle(vehicleId: number): Promise<VehicleResponse> {
  return request<VehicleResponse>(`/me/vehicles/${vehicleId}`);
}

export async function createVehicle(data: VehicleRequest): Promise<VehicleResponse> {
  return request<VehicleResponse>('/me/vehicles', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updateVehicle(
  vehicleId: number,
  data: Partial<VehicleRequest>,
): Promise<VehicleResponse> {
  return request<VehicleResponse>(`/me/vehicles/${vehicleId}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deleteVehicle(vehicleId: number): Promise<void> {
  await request(`/me/vehicles/${vehicleId}`, {
    method: 'DELETE',
  });
}