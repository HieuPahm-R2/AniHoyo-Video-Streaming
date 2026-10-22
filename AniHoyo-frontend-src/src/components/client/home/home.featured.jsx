import Carousel from "react-multi-carousel";
import "react-multi-carousel/lib/styles.css";
import React from 'react'
import { Col, Row } from "antd";
import { FireFilled, FireOutlined, FireTwoTone } from "@ant-design/icons";

const responsive = {
    desktop: {
        breakpoint: { max: 3000, min: 1200 },
        items: 3,
        slidesToSlide: 1
    },
    tablet: {
        breakpoint: { max: 1200, min: 768 },
        items: 2,
        slidesToSlide: 1
    },
    mobile: {
        breakpoint: { max: 768, min: 0 },
        items: 1,
        slidesToSlide: 1
    }
};

const Featured = () => {
    const featuredMovies = [
        {
            id: 1,
            title: "The Lost City",
            image: "/Tokubet.webp",
            genres: ["Action", "Thriller"],
            rating: 8.4,
            quality: "HD",
            ageRating: "16+",
            description: "When a renowned archaeologist goes missing, his daughter sets out on a perilous journey to the heart of the Amazon rainforest to find him. Along the way, she discovers a hidden city and..."
        },
        {
            id: 2,
            title: "Undercurrents",
            image: "/demon1.webp",
            genres: ["Comedy"],
            rating: 7.1,
            quality: "FHD",
            ageRating: "18+",
            description: "A brilliant scientist discovers a way to harness the power of the ocean's currents to create a new, renewable energy source. But when her groundbreaking technology falls into the wrong hands..."
        },
        {
            id: 3,
            title: "Redemption Road",
            image: "/hero1.jpg",
            genres: ["Romance", "Drama", "Music"],
            rating: 6.3,
            quality: "HD",
            ageRating: "12+",
            description: "A down-on-his-luck boxer struggles to make ends meet while raising his young son. When an old friend offers him a chance to make some quick cash by fighting in an illegal underground boxing..."
        },
        {
            id: 4,
            title: "Summer 2025",
            image: "/summer2025.jpg",
            genres: ["Adventure", "Sci-Fi"],
            rating: 7.8,
            quality: "4K",
            ageRating: "13+",
            description: "In the year 2025, a group of explorers discovers a mysterious portal that leads to parallel dimensions. They must navigate through dangerous worlds to find their way back home..."
        }
    ];

    return (
        <div className="home home--static">
            <Row>
                <Col span={6}>
                    <div className="section__header">
                        <FireFilled style={{ color: "orange" }} />
                        <h2 className="section__title">Nổi Bật Nhất</h2>
                    </div>
                </Col>
            </Row>
            <div className="home__carousel" id="flixtv-hero">
                <Carousel
                    responsive={responsive}
                    removeArrowOnDeviceType={["tablet", "mobile"]}
                >
                    {featuredMovies.map((movie) => (
                        <div key={movie.id} className="home__card">
                            <div className="home__card-image">
                                <img src={movie.image} alt={movie.title} />
                                <div className="home__card-overlay">
                                    <span className="home__card-type">The Movie</span>
                                    <p className="home__card-placeholder">
                                        Many desktop publishing packages and web page editors now use lorem ipsum as their default model text.
                                    </p>
                                </div>
                            </div>
                            <div className="home__card-content">
                                <h3 className="home__card-title">{movie.title}</h3>
                                <div className="home__card-genres">
                                    {movie.genres.map((genre, index) => (
                                        <span key={index} className="home__card-genre">{genre}</span>
                                    ))}
                                </div>
                                <div className="home__card-rating">
                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" className="star-icon">
                                        <path d="M22,9.67A1,1,0,0,0,21.14,9l-5.69-.83L12.9,3a1,1,0,0,0-1.8,0L8.55,8.16,2.86,9a1,1,0,0,0-.81.68,1,1,0,0,0,.25,1l4.13,4-1,5.68A1,1,0,0,0,6.9,21.44L12,18.77l5.1,2.67a.93.93,0,0,0,.46.12,1,1,0,0,0,.59-.19,1,1,0,0,0,.4-1l-1-5.68,4.13-4A1,1,0,0,0,22,9.67Zm-6.15,4a1,1,0,0,0-.29.88l.72,4.2-3.76-2a1.06,1.06,0,0,0-.94,0l-3.76,2,.72-4.2a1,1,0,0,0-.29-.88l-3-3,4.21-.61a1,1,0,0,0,.76-.55L12,5.7l1.88,3.82a1,1,0,0,0,.76.55l4.21.61Z" />
                                    </svg>
                                    <span className="rating-value">{movie.rating}</span>
                                </div>
                                <div className="home__card-tags">
                                    <span className="home__card-quality">{movie.quality}</span>
                                    <span className="home__card-age">{movie.ageRating}</span>
                                </div>
                                <p className="home__card-description">{movie.description}</p>
                            </div>
                        </div>
                    ))}
                </Carousel>
            </div>
        </div>
    )
}

export default Featured