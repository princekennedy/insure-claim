export interface VehicleRequest {
  registrationNumber: string;
  make: string;
  model: string;
  year: number;
  color?: string;
  chassisNumber?: string;
  engineNumber?: string;
}

export interface VehicleResponse {
  id: number;
  registrationNumber: string;
  make: string;
  model: string;
  year: number;
  color: string | null;
  chassisNumber: string | null;
  engineNumber: string | null;
}

export function formatVehicle(vehicle: VehicleResponse): string {
  return `${vehicle.year} ${vehicle.make} ${vehicle.model}`;
}

export function formatVehicleShort(vehicle: VehicleResponse): string {
  return `${vehicle.registrationNumber} (${vehicle.make} ${vehicle.model})`;
}
