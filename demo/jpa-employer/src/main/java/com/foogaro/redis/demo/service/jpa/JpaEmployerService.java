package com.foogaro.redis.demo.service.jpa;

import com.foogaro.redis.demo.entity.Employer;
import com.foogaro.redis.demo.repository.jpa.JpaEmployerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JpaEmployerService {

    @Autowired
    private JpaEmployerRepository repository;

    public List<Employer> getAllEmployers() {
        return repository.findAll();
    }

    public Optional<Employer> getEmployerById(Long id) {
        return repository.findById(id);
    }

    public Employer saveEmployer(Employer employer) {
        return repository.save(employer);
    }

    public void deleteEmployer(Long id) {
        repository.deleteById(id);
    }

    public Employer getEmployerByEmail(String email) {
        return repository.findByEmail(email);
    }
}
