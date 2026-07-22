package com.socialnetwork.social.repository;

import com.socialnetwork.social.entity.OtpCode;
import com.socialnetwork.social.entity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    // آخرین کد فعال (استفاده‌نشده) برای یک شناسه، صرف‌نظر از هدف؛ چون یک شناسه در هر لحظه فقط یک کد معتبر دارد
    Optional<OtpCode> findFirstByIdentifierAndUsedFalseOrderByCreatedAtDesc(String identifier);

    // برای محدود کردن ارسال مکرر: آخرین کد ارسالی برای این شناسه بدون توجه به وضعیت استفاده
    Optional<OtpCode> findFirstByIdentifierOrderByCreatedAtDesc(String identifier);
}