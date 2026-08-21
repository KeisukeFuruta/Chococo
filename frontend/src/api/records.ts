import { apiRequest } from "./client";
import type { RecordDetail, RecordListItem } from "../types";

export interface RecordListResponseDto {
  records: RecordListItem[];
}

export interface RecordInput {
  sweetName: string;
  recordDate: string;
  comment: string;
  photoFile?: File | null;
}

export function listRecords(year: number, month: number): Promise<RecordListItem[]> {
  return apiRequest<RecordListResponseDto>(`/records?year=${year}&month=${month}`).then((res) => res.records);
}

export function getRecord(id: number): Promise<RecordDetail> {
  return apiRequest<RecordDetail>(`/records/${id}`);
}

export function createRecord(input: RecordInput, pairingSuggestionId?: number): Promise<RecordDetail> {
  const formData = new FormData();
  formData.append("sweetName", input.sweetName);
  formData.append("recordDate", input.recordDate);
  if (input.comment) formData.append("comment", input.comment);
  if (pairingSuggestionId !== undefined) {
    formData.append("pairingSuggestionId", String(pairingSuggestionId));
  }
  if (input.photoFile) formData.append("photo", input.photoFile);

  return apiRequest<RecordDetail>("/records", { method: "POST", body: formData });
}

export function updateRecord(id: number, input: RecordInput, removePhoto: boolean): Promise<RecordDetail> {
  const formData = new FormData();
  formData.append("sweetName", input.sweetName);
  formData.append("recordDate", input.recordDate);
  if (input.comment) formData.append("comment", input.comment);
  if (removePhoto) formData.append("deletePhoto", "true");
  else if (input.photoFile) formData.append("photo", input.photoFile);

  return apiRequest<RecordDetail>(`/records/${id}`, { method: "PUT", body: formData });
}

export function deleteRecord(id: number): Promise<void> {
  return apiRequest<void>(`/records/${id}`, { method: "DELETE" });
}
