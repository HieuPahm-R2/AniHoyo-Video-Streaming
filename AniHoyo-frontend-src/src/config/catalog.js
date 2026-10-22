import { useEffect, useState } from 'react';
import { fetchFilmCategory, fetchFilmTags } from '@/config/api.handle';

/**
 * Catalog dùng chung cho form phim: Thể loại (Category) + Highlight Tag.
 *
 * Category/Tag là dữ liệu DB (quan hệ FK theo id) nên KHÔNG hardcode ở FE — nhưng
 * chúng rất ít đổi, vì vậy chỉ fetch MỘT lần cho cả app rồi chia sẻ lại, thay vì mỗi
 * lần mở modal phim lại gọi API 2 lần như trước.
 *
 * Sau khi CRUD ở trang admin (table-category / table-tag) thì gọi
 * `invalidateFilmCatalog()` để lần mở modal sau lấy dữ liệu mới.
 */

const EMPTY = { categories: [], tags: [] };

let cache = null;
let inflight = null;

const toOptions = (rows, labelField) =>
  (rows ?? []).map((item) => ({ label: item[labelField], value: item.id }));

export const loadFilmCatalog = () => {
  if (cache) return Promise.resolve(cache);
  if (!inflight) {
    inflight = Promise.all([fetchFilmCategory(), fetchFilmTags()])
      .then(([categoryRes, tagRes]) => {
        cache = {
          categories: toOptions(categoryRes?.data?.result, 'name'),
          tags: toOptions(tagRes?.data?.result, 'name'),
        };
        return cache;
      })
      .finally(() => {
        inflight = null;
      });
  }
  return inflight;
};

export const invalidateFilmCatalog = () => {
  cache = null;
  inflight = null;
};

export const useFilmCatalog = () => {
  const [state, setState] = useState(
    cache ? { loading: false, ...cache } : { loading: true, ...EMPTY }
  );

  useEffect(() => {
    if (cache) return;
    let alive = true;
    loadFilmCatalog()
      .then((data) => {
        if (alive) setState({ loading: false, ...data });
      })
      .catch(() => {
        if (alive) setState({ loading: false, ...EMPTY });
      });
    return () => {
      alive = false;
    };
  }, []);

  return state;
};
