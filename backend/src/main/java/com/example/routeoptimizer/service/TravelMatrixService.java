package com.example.routeoptimizer.service;

import com.example.routeoptimizer.entity.TravelMatrixEntity;
import com.example.routeoptimizer.exception.ValidationException;
import com.example.routeoptimizer.model.Area;
import com.example.routeoptimizer.model.TravelMatrix;
import com.example.routeoptimizer.repository.TravelMatrixRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TravelMatrixService {

    private final TravelMatrixRepository travelMatrixRepository;
    private final TravelMatrix travelMatrix;

    public TravelMatrixService(
            TravelMatrixRepository travelMatrixRepository,
            @Value("${optimizer.same-area-travel-buffer-minutes:10}") int defaultBufferMinutes) {
        this.travelMatrixRepository = travelMatrixRepository;
        this.travelMatrix = new TravelMatrix();
        this.travelMatrix.setDefaultSameAreaBufferMinutes(defaultBufferMinutes);
    }

    public TravelMatrix getTravelMatrix() {
        syncFromRepository();
        return this.travelMatrix;
    }

    public int getTravelTime(Area areaA, Area areaB) {
        if (areaA == null || areaB == null) {
            throw new ValidationException("Areas must not be null");
        }
        syncFromRepository();
        return travelMatrix.getTravelTime(areaA, areaB);
    }

    @Transactional
    public synchronized void updateTravelTime(Area areaA, Area areaB, int minutes) {
        if (areaA == null || areaB == null) {
            throw new ValidationException("Areas must not be null");
        }
        if (minutes < 0) {
            throw new ValidationException("Travel time minutes cannot be negative");
        }

        travelMatrix.setTravelTime(areaA, areaB, minutes);
        String key = TravelMatrix.buildKey(areaA, areaB);
        travelMatrixRepository.save(TravelMatrixEntity.builder()
                .areaKey(key)
                .areaA(areaA)
                .areaB(areaB)
                .travelTimeMinutes(minutes)
                .build());
    }

    @Transactional
    public synchronized void initializeDefaultDhakaMatrix() {
        setSymmetric(Area.UTTARA, Area.BANANI, 25);
        setSymmetric(Area.UTTARA, Area.GULSHAN, 30);
        setSymmetric(Area.UTTARA, Area.BASHUNDHARA, 20);
        setSymmetric(Area.UTTARA, Area.MIRPUR, 35);
        setSymmetric(Area.UTTARA, Area.MOHAMMADPUR, 45);
        setSymmetric(Area.UTTARA, Area.DHANMONDI, 50);
        setSymmetric(Area.UTTARA, Area.MOTIJHEEL, 60);
        setSymmetric(Area.UTTARA, Area.OLD_DHAKA, 65);
        setSymmetric(Area.UTTARA, Area.JATRABARI, 70);

        setSymmetric(Area.BANANI, Area.GULSHAN, 10);
        setSymmetric(Area.BANANI, Area.BASHUNDHARA, 15);
        setSymmetric(Area.BANANI, Area.MIRPUR, 25);
        setSymmetric(Area.BANANI, Area.MOHAMMADPUR, 30);
        setSymmetric(Area.BANANI, Area.DHANMONDI, 35);
        setSymmetric(Area.BANANI, Area.MOTIJHEEL, 40);
        setSymmetric(Area.BANANI, Area.OLD_DHAKA, 50);
        setSymmetric(Area.BANANI, Area.JATRABARI, 45);

        setSymmetric(Area.GULSHAN, Area.BASHUNDHARA, 15);
        setSymmetric(Area.GULSHAN, Area.MIRPUR, 30);
        setSymmetric(Area.GULSHAN, Area.MOHAMMADPUR, 35);
        setSymmetric(Area.GULSHAN, Area.DHANMONDI, 35);
        setSymmetric(Area.GULSHAN, Area.MOTIJHEEL, 35);
        setSymmetric(Area.GULSHAN, Area.OLD_DHAKA, 45);
        setSymmetric(Area.GULSHAN, Area.JATRABARI, 40);

        setSymmetric(Area.BASHUNDHARA, Area.MIRPUR, 40);
        setSymmetric(Area.BASHUNDHARA, Area.MOHAMMADPUR, 45);
        setSymmetric(Area.BASHUNDHARA, Area.DHANMONDI, 45);
        setSymmetric(Area.BASHUNDHARA, Area.MOTIJHEEL, 45);
        setSymmetric(Area.BASHUNDHARA, Area.OLD_DHAKA, 55);
        setSymmetric(Area.BASHUNDHARA, Area.JATRABARI, 50);

        setSymmetric(Area.MIRPUR, Area.MOHAMMADPUR, 20);
        setSymmetric(Area.MIRPUR, Area.DHANMONDI, 30);
        setSymmetric(Area.MIRPUR, Area.MOTIJHEEL, 45);
        setSymmetric(Area.MIRPUR, Area.OLD_DHAKA, 50);
        setSymmetric(Area.MIRPUR, Area.JATRABARI, 55);

        setSymmetric(Area.MOHAMMADPUR, Area.DHANMONDI, 15);
        setSymmetric(Area.MOHAMMADPUR, Area.MOTIJHEEL, 35);
        setSymmetric(Area.MOHAMMADPUR, Area.OLD_DHAKA, 40);
        setSymmetric(Area.MOHAMMADPUR, Area.JATRABARI, 45);

        setSymmetric(Area.DHANMONDI, Area.MOTIJHEEL, 25);
        setSymmetric(Area.DHANMONDI, Area.OLD_DHAKA, 30);
        setSymmetric(Area.DHANMONDI, Area.JATRABARI, 35);

        setSymmetric(Area.MOTIJHEEL, Area.OLD_DHAKA, 15);
        setSymmetric(Area.MOTIJHEEL, Area.JATRABARI, 20);

        setSymmetric(Area.OLD_DHAKA, Area.JATRABARI, 25);
    }

    private void syncFromRepository() {
        List<TravelMatrixEntity> entities = travelMatrixRepository.findAll();
        for (TravelMatrixEntity e : entities) {
            travelMatrix.setTravelTime(e.getAreaA(), e.getAreaB(), e.getTravelTimeMinutes());
        }
    }

    private void setSymmetric(Area a, Area b, int minutes) {
        travelMatrix.setTravelTime(a, b, minutes);
        String key = TravelMatrix.buildKey(a, b);
        travelMatrixRepository.save(TravelMatrixEntity.builder()
                .areaKey(key)
                .areaA(a)
                .areaB(b)
                .travelTimeMinutes(minutes)
                .build());
    }
}
