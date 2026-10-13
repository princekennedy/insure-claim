export type SettingDataType = 'STRING' | 'NUMBER' | 'BOOLEAN';

export interface SettingValue {
  key: string;
  value: string;
  dataType: SettingDataType;
  description: string;
}

export interface SettingGroup {
  group: string;
  settings: SettingValue[];
}

export interface SettingsResponse {
  groups: SettingGroup[];
}

export interface SettingUpdate {
  key: string;
  value: string;
}

export interface SettingGroupUpdate {
  group: string;
  settings: SettingUpdate[];
}

export interface SettingsUpdateRequest {
  groups: SettingGroupUpdate[];
}
