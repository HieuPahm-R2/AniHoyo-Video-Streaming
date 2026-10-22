export interface IBackendRes<T> {
  error?: string | string[];
  message: string;
  statusCode: number | string;
  data?: T;
}


export interface IModelPaginate<T> {
  meta: {
    page: number;
    pageSize: number;
    pages: number;
    total: number;
  };
  result: T[];
}
export interface IUser {
  id?: string;
  fullName: string;
  email: string;
  password?: string;
  role?: {
    id: string;
    name: string;
  };
  createdBy?: string;
  createdTime?: string;
  updatedTime?: string;
}
export interface IPermission {
  id?: string;
  name?: string;
  apiPath?: string;
  method?: string;
  module?: string;

  createdBy?: string;

  createdTime?: string;
  updatedTime?: string;
}

export interface IRole {
  id?: string;
  name: string;
  description: string;
  active: boolean;
  permissions: IPermission[] | string[];

  createdBy?: string;
  createdTime?: string;
  updatedTime?: string;
}

/** Thể loại phim (bảng categories) — quan hệ nhiều-nhiều với Film theo id. */
export interface ICategory {
  id?: string;
  name: string;
}

/** Highlight tag của phim (bảng tags) — quan hệ nhiều-nhiều với Film theo id. */
export interface ITag {
  id?: string;
  name: string;
}