import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { callFetchUser } from '@/config/api';
import { IModelPaginate, IUser } from '@/types/backend';

interface IState {
    isFetching: boolean;
    meta: {
        page: number;
        pageSize: number;
        pages: number;
        total: number;
    },
    result: IUser[]
}
// First, create the thunk
export const fetchUser = createAsyncThunk(
    'user/fetchUser',
    async ({ query }: { query: string }) => {
        const response = await callFetchUser(query);
        return response;
    }
)

const initialState: IState = {
    isFetching: true,
    meta: {
        page: 1,
        pageSize: 10,
        pages: 0,
        total: 0
    },
    result: []
};


export const userSlice = createSlice({
    name: 'user',
    initialState,
    reducers: {
        // Use the PayloadAction type to declare the contents of `action.payload`
        setActiveMenu: (state, action) => {
            // state.activeMenu = action.payload;
        },

    },
    extraReducers: (builder) => {
        // Add reducers for additional action types here, and handle loading state as needed
        builder.addCase(fetchUser.pending, (state, action) => {
            state.isFetching = true;

        })

        builder.addCase(fetchUser.rejected, (state, action) => {
            state.isFetching = false;

        })

        builder.addCase(fetchUser.fulfilled, (state, action) => {
            const payload = action.payload;
            if (payload && payload.data) {
                //Cast payload.data as IModelPaginate<IRole> before reading meta/result.
                const pageData = payload.data as unknown as IModelPaginate<IUser>;
                state.isFetching = false;
                state.meta = pageData.meta;
                state.result = pageData.result;
            }

            // state.courseOrder = action.payload;
        })
    },

});

export const {
    setActiveMenu,
} = userSlice.actions;

export default userSlice.reducer;
