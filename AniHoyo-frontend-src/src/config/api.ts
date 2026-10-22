import instance from './axios.customize';
import { IBackendRes, ICategory, IModelPaginate, IPermission, IRole, ITag, IUser } from '@/types/backend';

export const callCreateRole = (role: IRole): Promise<IBackendRes<IRole>> => {
    return instance.post('/api/v1/add-role', { ...role })
}

export const callUpdateRole = (role: IRole, id: string): Promise<IBackendRes<IRole>> => {
    return instance.put(`/api/v1/update-role`, { id, ...role })
}

export const callDeleteRole = (id: string): Promise<IBackendRes<IRole>> => {
    return instance.delete(`/api/v1/delete-role/${id}`);
}
export const callFetchRole = (query: string): Promise<IBackendRes<IModelPaginate<IRole>>> => {
    return instance.get(`/api/v1/roles?${query}`);
}

export const callFetchRoleById = (id: string): Promise<IBackendRes<IRole>> => {
    return instance.get(`/api/v1/role/${id}`);
}
/**
Module Permission
 */
export const callCreatePermission = (permission: IPermission): Promise<IBackendRes<IPermission>> => {
    return instance.post('/api/v1/add-permission', { ...permission })
}
export const callUpdatePermission = (permission: IPermission, id: string): Promise<IBackendRes<IPermission>> => {
    return instance.put(`/api/v1/update-permission`, { id, ...permission })
}
export const callDeletePermission = (id: string): Promise<IBackendRes<IPermission>> => {
    return instance.delete(`/api/v1/delete-permission/${id}`);
}
export const callFetchPermission = (query: string): Promise<IBackendRes<IModelPaginate<IPermission>>> => {
    return instance.get(`/api/v1/permissions?${query}`);
}
/**
 * 
Module User
 */
export const callCreateUser = (user: IUser): Promise<IBackendRes<IUser>> => {
    return instance.post('/api/v1/add-user', { ...user })
}

export const callUpdateUser = (user: IUser): Promise<IBackendRes<IUser>> => {
    return instance.put(`/api/v1/update-user`, { ...user })
}

export const callDeleteUser = (id: string): Promise<IBackendRes<IUser>> => {
    return instance.delete(`/api/v1/delete-user/${id}`);
}

export const callFetchUser = (query: string): Promise<IBackendRes<IModelPaginate<IUser>>> => {
    return instance.get(`/api/v1/users?${query}`);
}

/**
 * Module Category (thể loại phim) và Tag (highlight tag)
 * Lưu ý: update gửi kèm `id` trong body (controller nhận CategoryDTO/TagDTO).
 */
export const callCreateCategory = (category: ICategory): Promise<IBackendRes<ICategory>> => {
    return instance.post('/api/v1/add-category', { ...category })
}

export const callUpdateCategory = (category: ICategory): Promise<IBackendRes<ICategory>> => {
    return instance.put('/api/v1/update-category', { ...category })
}

export const callDeleteCategory = (id: string): Promise<IBackendRes<ICategory>> => {
    return instance.delete(`/api/v1/delete-category/${id}`);
}

export const callFetchCategory = (query: string): Promise<IBackendRes<IModelPaginate<ICategory>>> => {
    return instance.get(`/api/v1/categories?${query}`);
}

export const callCreateTag = (tag: ITag): Promise<IBackendRes<ITag>> => {
    return instance.post('/api/v1/add-tag', { ...tag })
}

export const callUpdateTag = (tag: ITag): Promise<IBackendRes<ITag>> => {
    return instance.put('/api/v1/update-tag', { ...tag })
}

export const callDeleteTag = (id: string): Promise<IBackendRes<ITag>> => {
    return instance.delete(`/api/v1/delete-tag/${id}`);
}

export const callFetchTag = (query: string): Promise<IBackendRes<IModelPaginate<ITag>>> => {
    return instance.get(`/api/v1/tags?${query}`);
}


