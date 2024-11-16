package com.foogaro.redis.demo.controller;

import com.foogaro.redis.demo.entity.Employer;
import com.foogaro.redis.demo.service.redis.RedisEmployerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/employers")
public class EmployerController {

    @Autowired
    private RedisEmployerService redisEmployerService;

    @GetMapping
    public Iterable<Employer> findAll() {
        return redisEmployerService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employer> findById(@PathVariable Long id) {
        Optional<Employer> employer = redisEmployerService.findById(id);
        return employer.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public void saveEmployer(@RequestBody Employer employer) {
        redisEmployerService.save(employer);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateEmployer(@PathVariable Long id, @RequestBody Employer updatedEmployer) {
        Optional<Employer> existingEmployerOpt = redisEmployerService.findById(id);
        if (existingEmployerOpt.isPresent()) {
            Employer existingEmployer = existingEmployerOpt.get();

            existingEmployer.setName(updatedEmployer.getName());
            existingEmployer.setAddress(updatedEmployer.getAddress());
            existingEmployer.setEmail(updatedEmployer.getEmail());
            existingEmployer.setPhone(updatedEmployer.getPhone());

            redisEmployerService.save(existingEmployer);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployer(@PathVariable Long id) {
        redisEmployerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
