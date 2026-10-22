import React, { useEffect, useState } from 'react'
import { FaStar } from "react-icons/fa";
import { IoIosPlayCircle } from "react-icons/io";
import '@/assets/styles/Banner.css';
import { Carousel } from 'antd';
import { fetchAllSeasons } from '@/config/api.handle';
import { convertSlug } from '@/config/utils';
import { useNavigate } from 'react-router-dom';

const Banner = () => {
    const [current, setCurrent] = useState(0);
    const [pageSize, setPageSize] = useState(5);
    const [sortQuery, setSortQuery] = useState("sort=uploadDate,desc");
    const [listFilm, setListFilm] = useState([])
    const navigate = useNavigate();

    const handleRedirect = (item) => {
        const slug = convertSlug(item.seasonName);
        navigate(`/detail/${slug}?id=${item.id}`)
    }

    useEffect(() => {
        const fetchFilms = async () => {

            let queryString = `page=${current}&size=${pageSize}`;
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
        <Carousel arrows autoplay={{ dotDuration: true }} autoplaySpeed={5000}>
            {listFilm.map((movie, index) => (
                <div className='banner-container' >
                    <img src={`${import.meta.env.VITE_BACKEND_URL}/storage/slider/${movie.film.slider}`} alt="" className="bg-film" />
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <div className="content-view">
                            <h1 className="content-title">{movie.seasonName}</h1>
                            <p>{movie.description}</p>
                            <div className="content-details">
                                <h6 className="cotent-studio">Studio: {movie.film.studio}</h6>
                                <h5 className="content-tags">{movie.film.categories.map(item => item.name)}</h5>
                                <h4 className="content-releaseDate">{movie.releaseYear}</h4>
                                <h3 className="content-rates">
                                    <span>IDMB</span><FaStar /> 9.6
                                </h3>
                            </div>
                            <div className="btn-action" onClick={() => handleRedirect(movie)}>
                                <a >
                                    <IoIosPlayCircle className="play-btn" />
                                    <span> Xem ngay</span>
                                </a>
                            </div>
                        </div>
                        <div className="out-banner">
                            <img src={`${import.meta.env.VITE_BACKEND_URL}/storage/visual/${movie.thumb}`} alt="" />
                        </div>
                    </div>
                </div >
            ))}


        </Carousel>

    )
}

export default Banner