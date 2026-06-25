import { http, type ApiResponse } from './http'

export interface LoginReq {
  username: string
  password: string
}

export interface UserInfo {
  id: number
  username: string
  realName: string
  roles: string[]
}

export interface LoginResult {
  token: string
  user: UserInfo
}

export function login(data: LoginReq) {
  return http.post<ApiResponse<LoginResult>>('/auth/login', data)
}

export function getCurrentUser() {
  return http.get<ApiResponse<UserInfo>>('/auth/me')
}
