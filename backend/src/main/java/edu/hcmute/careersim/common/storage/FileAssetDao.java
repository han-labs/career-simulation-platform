package edu.hcmute.careersim.common.storage;

import edu.hcmute.careersim.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class FileAssetDao {
    private final JdbcTemplate jdbc;

    record Row(long owner, Long enterprise, String key, FileAssetService.Asset asset) {}

    Row get(long id) {
        return get(id, false);
    }

    Row get(long id, boolean lock) {
        return jdbc
                .query(
                        "SELECT owner_user_id,enterprise_id,storage_key,id,original_name,mime_type,size_bytes,checksum,state FROM public.file_asset WHERE id=?"
                                + (lock ? " FOR SHARE" : ""),
                        (r, n) ->
                                new Row(
                                        r.getLong(1),
                                        r.getObject(2, Long.class),
                                        r.getString(3),
                                        new FileAssetService.Asset(
                                                Long.toString(r.getLong(4)),
                                                r.getString(5),
                                                r.getString(6),
                                                r.getLong(7),
                                                r.getString(8),
                                                r.getString(9))),
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Asset not found."));
    }

    long stage(
            long owner,
            Long enterprise,
            String key,
            String name,
            String mime,
            long size,
            String hash) {
        return jdbc.queryForObject(
                "INSERT INTO public.file_asset(owner_user_id,enterprise_id,storage_key,original_name,mime_type,size_bytes,checksum) VALUES (?,?,?,?,?,?,?) RETURNING id",
                Long.class,
                owner,
                enterprise,
                key,
                name,
                mime,
                size,
                hash);
    }

    void ready(long id) {
        jdbc.update(
                "UPDATE public.file_asset SET state='READY',ready_at=CURRENT_TIMESTAMP,row_version=row_version+1 WHERE id=? AND state='STAGED'",
                id);
    }
}
