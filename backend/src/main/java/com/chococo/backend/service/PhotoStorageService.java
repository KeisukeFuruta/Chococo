package com.chococo.backend.service;

import com.chococo.backend.exception.InvalidPhotoException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// aws-infra-design.md 3.4節：連番IDではなくUUIDファイル名でディスク保存し、URL推測による他人の画像への到達を防ぐ。
// サイズ上限（5MB）はspring.servlet.multipart.max-file-size側でチェックする（超過時はMaxUploadSizeExceededException）
@Service
public class PhotoStorageService {

    private final Path uploadDir;

    public PhotoStorageService(@Value("${chococo.storage.upload-dir}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir);
    }

    public String store(MultipartFile photo) {
        String extension = resolveExtension(photo.getContentType());
        String filename = UUID.randomUUID() + extension;
        try {
            Files.copy(photo.getInputStream(), uploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("写真の保存に失敗しました", e);
        }
        return "/uploads/" + filename;
    }

    // functional-spec.md 3.5節：呼び出し元（RecordPhotoCleanupListener）でAFTER_COMMIT後に呼ばれる想定。
    // 失敗してもDB側の削除・更新は既にコミット済みで巻き戻せないため、ここでは例外を投げるだけに留める
    public void delete(String photoPath) {
        String filename = Path.of(photoPath).getFileName().toString();
        try {
            Files.deleteIfExists(uploadDir.resolve(filename));
        } catch (IOException e) {
            throw new UncheckedIOException("写真の削除に失敗しました", e);
        }
    }

    private String resolveExtension(String contentType) {
        if (MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
            return ".jpg";
        }
        if (MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
            return ".png";
        }
        throw new InvalidPhotoException();
    }
}
