import DataTable from "@/components/admin/data.table";
import { IModelPaginate, ITag } from "@/types/backend";
import { DeleteOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons";
import { ActionType, ProColumns } from "@ant-design/pro-components";
import { Button, Popconfirm, Space, message, notification } from "antd";
import { useState, useRef } from "react";
import { callDeleteTag, callFetchTag } from "@/config/api";
import { invalidateFilmCatalog } from "@/config/catalog";
import ModalTag from "@/components/admin/tag/tag.modal";
import { ALL_PERMISSIONS } from "@/config/permission";
import Access from "@/components/share/access";
import queryString from "query-string";

const TagPage = () => {
    const [openModal, setOpenModal] = useState<boolean>(false);
    const [dataInit, setDataInit] = useState<ITag | null>(null);
    const [isFetching, setIsFetching] = useState<boolean>(false);

    const tableRef = useRef<ActionType>();

    const handleDeleteTag = async (id: string | undefined) => {
        if (id) {
            const res = await callDeleteTag(id);
            if (res && +res.statusCode === 200) {
                message.success("Xóa tag thành công");
                invalidateFilmCatalog();
                reloadTable();
            } else {
                notification.error({
                    message: "Có lỗi xảy ra",
                    description: (res as any).error ?? (res as any).message,
                });
            }
        }
    };

    const reloadTable = () => {
        tableRef?.current?.reload();
    };

    const columns: ProColumns<ITag>[] = [
        {
            title: "Id",
            dataIndex: "id",
            width: 80,
            sorter: true,
            hideInSearch: true,
        },
        {
            title: "Tên tag",
            dataIndex: "name",
            sorter: true,
        },
        {
            title: "Actions",
            hideInSearch: true,
            width: 50,
            render: (_value, entity, _index, _action) => (
                <Space>
                    <Access permission={ALL_PERMISSIONS.TAGS.UPDATE} hideChildren>
                        <EditOutlined
                            style={{
                                fontSize: 20,
                                color: "#ffa500",
                            }}
                            type=""
                            onClick={() => {
                                setOpenModal(true);
                                setDataInit(entity);
                            }}
                        />
                    </Access>

                    <Access permission={ALL_PERMISSIONS.TAGS.DELETE} hideChildren>
                        <Popconfirm
                            placement="leftTop"
                            title={"Xác nhận xóa tag"}
                            description={"Bạn có chắc chắn muốn xóa tag này ?"}
                            onConfirm={() => handleDeleteTag(entity.id)}
                            okText="Xác nhận"
                            cancelText="Hủy"
                        >
                            <span style={{ cursor: "pointer", margin: "0 10px" }}>
                                <DeleteOutlined
                                    style={{
                                        fontSize: 20,
                                        color: "#ff4d4f",
                                    }}
                                />
                            </span>
                        </Popconfirm>
                    </Access>
                </Space>
            ),
        },
    ];

    const buildQuery = (params: any, sort: any) => {
        const clone = { ...params };

        const parts: string[] = [];
        if (clone.name) parts.push(`name ~ '${clone.name}'`);

        clone.filter = parts.join(" and ");
        if (!clone.filter) delete clone.filter;

        clone.page = (clone.current || 1) - 1;
        clone.size = clone.pageSize;

        delete clone.current;
        delete clone.pageSize;
        delete clone.name;

        let temp = queryString.stringify(clone);

        let sortBy = "";
        if (sort && sort.name) {
            sortBy = sort.name === "ascend" ? "sort=name,asc" : "sort=name,desc";
        }

        // mặc định sort theo tên
        temp = sortBy ? `${temp}&${sortBy}` : `${temp}&sort=name,asc`;

        return temp;
    };

    return (
        <div>
            <Access permission={ALL_PERMISSIONS.TAGS.GET_PAGINATE}>
                <DataTable<ITag>
                    actionRef={tableRef}
                    headerTitle="Danh sách Tags (Highlight Tag)"
                    rowKey="id"
                    loading={isFetching}
                    columns={columns}
                    request={async (params, sort) => {
                        setIsFetching(true);
                        const query = buildQuery(params, sort);
                        const res = await callFetchTag(query);
                        setIsFetching(false);

                        const page = res.data as IModelPaginate<ITag> | undefined;
                        return {
                            data: page?.result ?? [],
                            total: page?.meta?.total ?? 0,
                            success: true,
                        };
                    }}
                    scroll={{ x: true }}
                    pagination={{
                        showSizeChanger: true,
                        showTotal: (total, range) => {
                            return (
                                <div>
                                    {" "}
                                    {range[0]}-{range[1]} trên {total} rows
                                </div>
                            );
                        },
                    }}
                    rowSelection={false}
                    toolBarRender={(_action, _rows): any => {
                        return (
                            <Button
                                icon={<PlusOutlined />}
                                type="primary"
                                onClick={() => setOpenModal(true)}
                            >
                                Thêm mới
                            </Button>
                        );
                    }}
                />
            </Access>
            <ModalTag
                openModal={openModal}
                setOpenModal={setOpenModal}
                reloadTable={reloadTable}
                dataInit={dataInit}
                setDataInit={setDataInit}
            />
        </div>
    );
};

export default TagPage;
