import { Col, Row } from 'antd'
import Carousel from "react-multi-carousel";
import "react-multi-carousel/lib/styles.css";
import React, { useEffect, useState } from 'react'
import { HistoryOutlined } from '@ant-design/icons';
import { fetchAllSeasons } from '@/config/api.handle';
import { SEASON_STATUS } from '@/config/constant.status';


const responsive = {
    desktop: {
        breakpoint: { max: 3000, min: 1024 },
        items: 6,
        slidesToSlide: 6 // optional, default to 1.
    },
    tablet: {
        breakpoint: { max: 1024, min: 464 },
        items: 4,
        slidesToSlide: 4 // optional, default to 1.
    },
    mobile: {
        breakpoint: { max: 464, min: 0 },
        items: 2,
        slidesToSlide: 2 // optional, default to 1.
    }
};

const CommingSoon = () => {
    const [current, setCurrent] = useState(0);
    const [pageSize, setPageSize] = useState(5);
    const [sortQuery, setSortQuery] = useState("sort=uploadDate,desc");
    const [listFilm, setListFilm] = useState([])

    useEffect(() => {
        const fetchFilms = async () => {

            let queryString = `page=${current}&size=${pageSize}&filter=status: '${SEASON_STATUS.COMMING_SOON}'`;
            if (sortQuery) {
                queryString += `&${sortQuery}`;
            }
            const res = await fetchAllSeasons(queryString);
            if (res && res.data) {
                setListFilm(res.data.result);
            }
        }
        fetchFilms();

    }, [current, pageSize, sortQuery]);
    return (
        <section class="section">
            <div class="container">
                <Row class="row">
                    <Col span={6}>
                        <div className="section__header">
                            <HistoryOutlined />
                            <h2 class="section__title">Phim Sắp Chiếu</h2>
                        </div>

                    </Col>

                    <Col span={24}>
                        <Carousel
                            responsive={responsive}
                            removeArrowOnDeviceType={["tablet", "mobile"]}
                        >
                            {listFilm.map((item, idx) => (
                                <div className='card--big'>
                                    <div class="card ">
                                        <a class="card__cover ">
                                            <img src={`${import.meta.env.VITE_BACKEND_URL}/storage/visual/${item.thumb}`} alt="" />
                                            <div className="card__overlay">
                                                <span className="card__year">{item.releaseYear}</span>
                                            </div>
                                        </a>
                                        <h3 class="card__title card__title--subs">
                                            <a>{item.seasonName}</a>
                                        </h3>
                                        <ul class="card__list card__list--subs">
                                            <li>Thanks for waiting...</li>
                                        </ul>
                                    </div>
                                </div>
                            ))}


                        </Carousel>

                    </Col>
                </Row>
            </div>
        </section>
    )
}

export default CommingSoon