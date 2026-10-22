import React, { useEffect, useState } from 'react'
import { Col, Divider, Flex, message, Rate, Row, Tag } from "antd";
import { useLocation, useNavigate } from 'react-router-dom';
import { fetchAveragePointAPI, fetchRatingAPI, fetchSeasonById } from '@/config/api.handle';
import { useDispatch, useSelector } from 'react-redux';
import { convertSlug } from '@/config/utils';
import CommentList from '@/components/client/feature/comment.list';

const DetailsPage = () => {
  const [seasonData, setSeasonData] = useState([]);
  const [rating, setRating] = useState(0);
  const [avgRating, setAvgRating] = useState(0);

  let location = useLocation();
  let params = new URLSearchParams(location.search);
  const id = params?.get("id"); // film id
  const navigate = useNavigate();

  const userId = useSelector(state => state.account.user.id);
  const username = useSelector(state => state.account.user.name);

  useEffect(() => {
    document.body.classList.add("client-theme");
    return () => {
      document.body.classList.remove("client-theme");
    };
  }, []);
  useEffect(() => {
    const fetchFilmById = async () => {
      const res = await fetchSeasonById(id);
      if (res && res.data) {
        let rawData = res.data;
        setTimeout(() => {
          setSeasonData(rawData);
        }, 1000);
      }
    }
    fetchAveragePointAPI(id)
      .then(res => {
        setAvgRating(res?.data);
        setRating(res?.data);
      })
      .catch((res) => { console.log(res) });
    fetchFilmById();
  }, [id]);

  const handleRedirect = (ss) => {
    const slug = convertSlug(ss.seasonName);
    navigate(`/watching/${slug}?id=${ss.id}`)
  }
  const handleChange = (value) => {
    setRating(value);
    fetchRatingAPI(userId, id, value)
      .then(() => {
        message.success("Đánh giá thành công!");
        return fetchAveragePointAPI(id);
      })
      .then(res => setAvgRating(res.data))
      .catch(() => message.error("Lỗi khi đánh giá"));
  };
  return (
    <section className="section section--head section--head-fixed section--gradient section--details-bg">
      <div className="section__bg" style={{ background: `url(${import.meta.env.VITE_BACKEND_URL}/storage/slider/${seasonData?.film?.slider}) center top / cover no-repeat` }}>
      </div>
      <div className="container">
        <div className="article">
          {/* Main Content Row */}
          <Row gutter={[15, 10]}>
            {/* Left Column - Main Content */}
            <Col span={24} xl={16}>
              <div className="article__content">
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                  <h1>{seasonData.seasonName}</h1>
                  <div style={{ display: "flex", flexDirection: "column", marginLeft: "10px" }}>
                    <h6 style={{ height: "5px" }}>Đánh giá bộ phim:</h6>
                    <Rate
                      onChange={handleChange}
                      count={10}
                      allowHalf
                      value={Number(rating)}
                      style={{ background: "white", padding: "5px", borderRadius: "5px" }}
                    />
                    <p style={{ height: "10px", marginTop: "-10px" }}>Điểm trung bình:  {avgRating} / 10</p>
                  </div>
                </div>


                <div style={{ display: "flex", alignItems: "center", }} >
                  <Tag color='cyan'> 9.7</Tag>
                  <Tag color='cyan'>Action</Tag>
                  <Tag color='cyan'>1 h 42 min</Tag>
                  <Tag color='cyan'>16+</Tag>
                </div>

                <p>{seasonData.description}</p>
              </div>
            </Col>

            {/* Right Column - Video Player */}
            <Col span={24} xl={8}>
              <div onClick={() => handleRedirect(seasonData)} style={{
                position: 'relative', borderRadius: '12px',
                overflow: 'hidden', boxShadow: '0 8px 32px rgba(0,0,0,0.3)',
                maxWidth: '220px', margin: '0 auto'
              }}>
                <div style={{ position: 'relative' }} >
                  <img
                    src={`${import.meta.env.VITE_BACKEND_URL}/storage/visual/${seasonData.thumb}`}
                    style={{
                      width: '100%',
                      height: '320px',
                      objectFit: 'cover',
                      display: 'block',
                      borderRadius: '12px'
                    }}
                    alt={seasonData.seasonName}
                  />

                  {/* Follow Button */}
                  <button
                    style={{
                      position: 'absolute',
                      top: '12px', left: '12px',
                      background: 'rgba(0,0,0,0.7)',
                      border: 'none', borderRadius: '6px',
                      padding: '6px 10px', color: 'white',
                      fontSize: '12px', fontWeight: '500', cursor: 'pointer',
                      display: 'flex', alignItems: 'center', gap: '4px', backdropFilter: 'blur(10px)', transition: 'all 0.3s ease'
                    }}
                    onMouseEnter={(e) => e.target.style.background = 'rgba(0,0,0,0.9)'}
                    onMouseLeave={(e) => e.target.style.background = 'rgba(0,0,0,0.7)'}
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M16,2H8A3,3,0,0,0,5,5V21a1,1,0,0,0,.5.87,1,1,0,0,0,1,0L12,18.69l5.5,3.18A1,1,0,0,0,18,22a1,1,0,0,0,.5-.13A1,1,0,0,0,19,21V5A3,3,0,0,0,16,2Zm1,17.27-4.5-2.6a1,1,0,0,0-1,0L7,19.27V5A1,1,0,0,1,8,4h8a1,1,0,0,1,1,1Z" />
                    </svg>
                    Theo dõi
                  </button>

                  {/* Play Button */}
                  <div
                    style={{
                      position: 'absolute',
                      top: '50%', left: '50%',
                      transform: 'translate(-50%, -50%)',
                      width: '60px', height: '60px',
                      background: 'rgba(255,255,255,0.9)',
                      borderRadius: '50%',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      cursor: 'pointer',
                      border: '2px solid rgba(0,0,0,0.2)',
                      transition: 'all 0.3s ease',
                      backdropFilter: 'blur(10px)'
                    }}
                    onMouseEnter={(e) => {
                      e.target.style.background = 'rgba(255,255,255,1)';
                      e.target.style.transform = 'translate(-50%, -50%) scale(1.1)';
                    }}
                    onMouseLeave={(e) => {
                      e.target.style.background = 'rgba(255,255,255,0.9)';
                      e.target.style.transform = 'translate(-50%, -50%) scale(1)';
                    }}

                  >
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="#000">
                      <path d="M8,5.14V19.14L19,12.14L8,5.14Z" />
                    </svg>
                  </div>

                  {/* Watch Movie Button */}
                  <button
                    style={{
                      position: 'absolute',
                      bottom: '15px', left: '50%', transform: 'translateX(-50%)',
                      background: 'linear-gradient(135deg, #ff4757, #ff3742)',
                      border: 'none', borderRadius: '6px', padding: '10px 20px',
                      color: 'white', fontSize: '14px', fontWeight: 'bold',
                      cursor: 'pointer', textTransform: 'uppercase',
                      letterSpacing: '0.5px', boxShadow: '0 4px 15px rgba(255,71,87,0.4)',
                      transition: 'all 0.3s ease',
                      minWidth: '240px'
                    }}
                    onMouseEnter={(e) => {
                      e.target.style.transform = 'translateX(-50%) translateY(-2px)';
                    }}
                    onMouseLeave={(e) => {
                      e.target.style.background = 'linear-gradient(135deg, #ff4757, #ff3742)';
                      e.target.style.boxShadow = '0 4px 15px rgba(255,71,87,0.4)';
                      e.target.style.transform = 'translateX(-50%) translateY(0)';
                    }}
                  >
                    XEM PHIM
                  </button>
                </div>
              </div>
            </Col>
          </Row>
          <Row gutter={[15, 10]} style={{ margin: '10px' }} >
            <div>
              <div  >
                <iframe
                  width={700}
                  height={400}
                  src={seasonData.trailer}
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                  referrerPolicy="strict-origin-when-cross-origin"
                  allowFullScreen
                ></iframe>
              </div>
            </div>

            <div className="article__actions article__actions--details">
              <div className="article__download">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path d="M21,14a1,1,0,0,0-1,1v4a1,1,0,0,1-1,1H5a1,1,0,0,1-1-1V15a1,1,0,0,0-2,0v4a3,3,0,0,0,3,3H19a3,3,0,0,0,3-3V15A1,1,0,0,0,21,14Zm-9.71,1.71a1,1,0,0,0,.33.21.94.94,0,0,0,.76,0,1,1,0,0,0,.33-.21l4-4a1,1,0,0,0-1.42-1.42L13,12.59V3a1,1,0,0,0-2,0v9.59l-2.29-2.3a1,1,0,1,0-1.42,1.42Z" /></svg>
                Trailer Film:
                <a href="#" download="#">1080p</a>
                <a href="#" download="#">FHD | SD | VietSub</a>
              </div>
            </div>
          </Row>

          {/* Series Section */}
          <Row gutter={[15, 10]} style={{ marginTop: '30px' }}>

            {/* Sidebar - Categories and Share */}
            <Col span={24} xl={8}>
              <div className="categories">
                <h3 className="categories__title">Thể Loại:</h3>

                {seasonData?.film?.categories?.map((category) => (
                  <a href="category.html" className="categories__item">{category.name}</a>
                ))}
              </div>

              <div className="share">
                <h3 className="share__title">Share</h3>
                <a href="#" className="share__link share__link--fb">
                  <svg width="9" height="17" viewBox="0 0 9 17" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M5.56341 16.8197V8.65888H7.81615L8.11468 5.84663H5.56341L5.56724 4.43907C5.56724 3.70559 5.63693 3.31257 6.69042 3.31257H8.09873V0.5H5.84568C3.1394 0.5 2.18686 1.86425 2.18686 4.15848V5.84695H0.499939V8.6592H2.18686V16.8197H5.56341Z" />
                  </svg> share
                </a>
                <a href="#" className="share__link share__link--tw">
                  <svg width="16" height="12" viewBox="0 0 16 12" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M7.55075 3.19219L7.58223 3.71122L7.05762 3.64767C5.14804 3.40404 3.47978 2.57782 2.06334 1.1902L1.37085 0.501686L1.19248 1.01013C0.814766 2.14353 1.05609 3.34048 1.843 4.14552C2.26269 4.5904 2.16826 4.65396 1.4443 4.38914C1.19248 4.3044 0.972149 4.24085 0.951164 4.27263C0.877719 4.34677 1.12953 5.31069 1.32888 5.69202C1.60168 6.22165 2.15777 6.74068 2.76631 7.04787L3.28043 7.2915L2.67188 7.30209C2.08432 7.30209 2.06334 7.31268 2.12629 7.53512C2.33613 8.22364 3.16502 8.95452 4.08833 9.2723L4.73884 9.49474L4.17227 9.8337C3.33289 10.321 2.34663 10.5964 1.36036 10.6175C0.888211 10.6281 0.5 10.6705 0.5 10.7023C0.5 10.8082 1.78005 11.4014 2.52499 11.6344C4.75983 12.3229 7.41435 12.0264 9.40787 10.8506C10.8243 10.0138 12.2408 8.35075 12.9018 6.74068C13.2585 5.88269 13.6152 4.315 13.6152 3.56293C13.6152 3.07567 13.6467 3.01212 14.2343 2.42953C14.5805 2.09056 14.9058 1.71983 14.9687 1.6139C15.0737 1.41264 15.0632 1.41264 14.5281 1.59272C13.6362 1.91049 13.5103 1.86812 13.951 1.39146C14.2762 1.0525 14.6645 0.438131 14.6645 0.258058C14.6645 0.22628 14.5071 0.279243 14.3287 0.374576C14.1398 0.480501 13.7202 0.639389 13.4054 0.734722L12.8388 0.914795L12.3247 0.565241C12.0414 0.374576 11.6427 0.162725 11.4329 0.0991699C10.8978 -0.0491255 10.0794 -0.0279404 9.59673 0.14154C8.2852 0.618204 7.45632 1.84694 7.55075 3.19219Z" />
                  </svg> tweet
                </a>

              </div>
            </Col>
          </Row>

          {/* Comments and Sidebar Section */}
          <Row gutter={[15, 10]} style={{ marginTop: '30px' }}>
            {/* Comments Section */}
            <Col span={24} xl={16}>
              <div className="comments">
                <ul className="nav nav-tabs comments__title comments__title--tabs" id="comments__tabs" role="tablist">
                  <li className="nav-item">
                    <a className="nav-link active" data-toggle="tab" href="#tab-1" role="tab" aria-controls="tab-1" aria-selected="true">
                      <h4>Comments</h4>
                    </a>
                  </li>
                </ul>

                <div className="tab-content">
                  <CommentList seasonId={id} userId={userId} username={username} />
                </div>
              </div>
            </Col>


          </Row>

        </div>
      </div>
    </section>
  )
}

export default DetailsPage
