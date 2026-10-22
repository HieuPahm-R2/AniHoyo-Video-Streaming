export const ALL_PERMISSIONS = {
    FILMS: {
        GET_PAGINATE: { method: "GET", apiPath: '/api/v1/films', module: "FILMS" },
        CREATE: { method: "POST", apiPath: '/api/v1/add-film', module: "FILMS" },
        UPDATE: { method: "PUT", apiPath: '/api/v1/upadte-film', module: "FILMS" },
        DELETE: { method: "DELETE", apiPath: '/api/v1/delete-film/{id}', module: "FILMS" },
    },
    SEASONS: {
        GET_PAGINATE: { method: "GET", apiPath: '/api/v1/seasons', module: "SEASONS" },
        CREATE: { method: "POST", apiPath: '/api/v1/add-season', module: "SEASONS" },
        UPDATE: { method: "PUT", apiPath: '/api/v1/update-season', module: "SEASONS" },
        DELETE: { method: "DELETE", apiPath: '/api/v1/delete-season/{id}', module: "SEASONS" },
    },
    PERMISSIONS: {
        GET_PAGINATE: { method: "GET", apiPath: '/api/v1/permissions', module: "PERMISSIONS" },
        CREATE: { method: "POST", apiPath: '/api/v1/add-permission', module: "PERMISSIONS" },
        UPDATE: { method: "PUT", apiPath: '/api/v1/update-permission', module: "PERMISSIONS" },
        DELETE: { method: "DELETE", apiPath: '/api/v1/delete-permission/{id}', module: "PERMISSIONS" },
    },
    TAGS: {
        GET_PAGINATE: { method: "GET", apiPath: '/api/v1/tags', module: "TAGS" },
        CREATE: { method: "POST", apiPath: '/api/v1/add-tag', module: "TAGS" },
        UPDATE: { method: "PUT", apiPath: '/api/v1/update-tag', module: "TAGS" },
        DELETE: { method: "DELETE", apiPath: '/api/v1/delete-tag/{id}', module: "TAGS" },
    },
    CATEGORIES: {
        GET_PAGINATE: { method: "GET", apiPath: '/api/v1/categories', module: "CATEGORIES" },
        CREATE: { method: "POST", apiPath: '/api/v1/add-category', module: "CATEGORIES" },
        UPDATE: { method: "PUT", apiPath: '/api/v1/update-category', module: "CATEGORIES" },
        DELETE: { method: "DELETE", apiPath: '/api/v1/delete-category/{id}', module: "CATEGORIES" },
    },
    ROLES: {
        GET_PAGINATE: { method: "GET", apiPath: '/api/v1/roles', module: "ROLES" },
        CREATE: { method: "POST", apiPath: '/api/v1/add-role', module: "ROLES" },
        UPDATE: { method: "PUT", apiPath: '/api/v1/update-role', module: "ROLES" },
        DELETE: { method: "DELETE", apiPath: '/api/v1/delete-role/{id}', module: "ROLES" },
    },
    USERS: {
        GET_PAGINATE: { method: "GET", apiPath: '/api/v1/users', module: "USERS" },
        CREATE: { method: "POST", apiPath: '/api/v1/add-user', module: "USERS" },
        UPDATE: { method: "PUT", apiPath: '/api/v1/update-user', module: "USERS" },
        DELETE: { method: "DELETE", apiPath: '/api/v1/delete-user/{id}', module: "USERS" },
    },
    RATINGS: {
        GET_AVERAGE: { method: "GET", apiPath: '/api/v1/ratings/average/{seasonId}', module: "RATINGS" },
        CREATE: { method: "POST", apiPath: '/api/v1/ratings', module: "RATINGS" },
    },
}