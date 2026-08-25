package com.stuttgarttaxi.taxihub.config;

import com.stuttgarttaxi.taxihub.entity.ExtraService;
import com.stuttgarttaxi.taxihub.entity.ExtraServiceCode;
import com.stuttgarttaxi.taxihub.repository.ExtraServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds the public booking site's extra-services catalog on first startup.
 * Child seats are free (legally required equipment in Germany, not a real
 * upsell, and split by age group like a real fleet would offer); minibar
 * drinks are flat-fee, non-alcoholic only.
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class ExtraServiceSeeder implements CommandLineRunner {

    private final ExtraServiceRepository extraServiceRepository;

    @Override
    public void run(String... args) {
        if (extraServiceRepository.count() > 0) {
            return;
        }

        extraServiceRepository.save(ExtraService.builder()
                .code(ExtraServiceCode.BABY_SEAT).label("Bebek Koltuğu (0-1 Yaş)").price(0.0).build());
        extraServiceRepository.save(ExtraService.builder()
                .code(ExtraServiceCode.CHILD_SEAT).label("Çocuk Koltuğu (1-4 Yaş)").price(0.0).build());
        extraServiceRepository.save(ExtraService.builder()
                .code(ExtraServiceCode.BOOSTER_SEAT).label("Yükseltici (4-12 Yaş)").price(0.0).build());
        extraServiceRepository.save(ExtraService.builder()
                .code(ExtraServiceCode.WATER).label("Su").price(2.0).build());
        extraServiceRepository.save(ExtraService.builder()
                .code(ExtraServiceCode.COLA).label("Kola").price(3.0).build());
        extraServiceRepository.save(ExtraService.builder()
                .code(ExtraServiceCode.SODA_LEMONADE).label("Soda / Limonata").price(3.0).build());
        extraServiceRepository.save(ExtraService.builder()
                .code(ExtraServiceCode.ORANGE_JUICE).label("Portakal Suyu").price(3.5).build());

        log.info("7 ek hizmet (3 çocuk koltuğu tipi, 4 alkolsüz içecek) oluşturuldu.");
    }
}
