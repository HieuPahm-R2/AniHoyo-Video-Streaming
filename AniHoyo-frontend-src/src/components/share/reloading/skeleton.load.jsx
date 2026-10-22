import { Col, Row, Skeleton } from "antd";

const WaitingContent = () => {
    return (
        <>
            <Row gutter={[15, 20]}>
                <Col md={10} sm={0} xs={0}>
                    <Skeleton.Input
                        active={true}
                        block={true}
                        style={{ width: 150, height: 800 }}
                        gradientFromColor="rgba(232, 224, 224, 0.9)"
                        gradientToColor="rgba(232, 224, 224, 0.9)"
                    />

                </Col>
                <Col md={14} sm={24}>
                    <Skeleton
                        paragraph={{ rows: 3 }}
                        active={true}
                    />
                    <br /> <br />

                    <br /> <br />
                    <div style={{ display: "flex", gap: 20, marginTop: 20, overflow: 'hidden' }}>
                        <Skeleton.Button active={true} style={{ width: 100, height: 200 }} />
                        <Skeleton.Button active={true} style={{ width: 100, height: 200 }} />
                        <Skeleton.Button active={true} style={{ width: 100, height: 200 }} />
                        <Skeleton.Button active={true} style={{ width: 100, height: 200 }} />
                        <Skeleton.Button active={true} style={{ width: 100, height: 200 }} />
                    </div>
                </Col>
            </Row>
        </>
    )
}
export default WaitingContent;