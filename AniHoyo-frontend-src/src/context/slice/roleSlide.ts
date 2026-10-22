import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import type { IRole, IModelPaginate } from '@/types/backend';
import { callFetchRole, callFetchRoleById } from '@/config/api';


interface IState {
    isFetching: boolean;
    meta: {
        page: number;
        pageSize: number;
        pages: number;
        total: number;
    },
    result: IRole[];
    isFetchSingle: boolean;
    singleRole: IRole
}
// create thunk
export const fetchRole = createAsyncThunk(
  'role/fetchRole',
  async ({ query }: { query: string }) => {
        const response = await callFetchRole(query);
        return response;
    }
)
export const fetchRoleById = createAsyncThunk(
    'role/fetchRoleById',
    async (id: string) => {
        const response = await callFetchRoleById(id);
        return response;
    }
)

const initialState: IState = {
    isFetching: true,
    isFetchSingle: true,
    meta: {
        page: 0,
        pageSize: 10,
        pages: 0,
        total: 0
    },
    result: [],
    singleRole: {
        id: "",
        name: "",
        description: "",
        active: false,
        permissions: []
    }
};


export const roleSlide = createSlice({
    name: 'role',
    initialState,
    // The `reducers` field lets us define reducers and generate associated actions
    reducers: {

        resetSingleRole: (state, action) => {
            state.singleRole = {
                id: "",
                name: "",
                description: "",
                active: false,
                permissions: []
            }
        },

    },
    extraReducers: (builder) => {
        // Add reducers for additional action types here, and handle loading state as needed
        builder.addCase(fetchRole.pending, (state, action) => {
            state.isFetching = true;
            // Add user to the state array
            // state.courseOrder = action.payload;
        })

        builder.addCase(fetchRole.rejected, (state, action) => {
            state.isFetching = false;
            // Add user to the state array
            // state.courseOrder = action.payload;
        })

        builder.addCase(fetchRole.fulfilled, (state, action) => {
            const payload = action.payload;
            if (payload && payload.data) {
                state.isFetching = false;
                //Cast payload.data as IModelPaginate<IRole> before reading meta/result.
                const pageData = payload.data as unknown as IModelPaginate<IRole>;
                state.meta = pageData.meta;
                state.result = pageData.result;
            }
            
        })

        builder.addCase(fetchRoleById.pending, (state, action) => {
            state.isFetchSingle = true;
            state.singleRole = {
                id: "",
                name: "",
                description: "",
                active: false,
                permissions: []
            }
        })

        builder.addCase(fetchRoleById.rejected, (state, action) => {
            state.isFetchSingle = false;
            state.singleRole = {
                id: "",
                name: "",
                description: "",
                active: false,
                permissions: []
            }
            // Add user to the state array
            // state.courseOrder = action.payload;
        })

        builder.addCase(fetchRoleById.fulfilled, (state, action) => {
            const payload = action.payload;
            if (payload && payload.data) {
                state.isFetchSingle = false;
                state.singleRole = payload.data as unknown as IRole;
            }
        })
    },

});

export const {
    resetSingleRole
} = roleSlide.actions;

export default roleSlide.reducer;


