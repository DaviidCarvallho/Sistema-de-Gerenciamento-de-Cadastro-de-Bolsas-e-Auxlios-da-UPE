package br.edu.upe.sgba.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevSeedData implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        // Dados de demonstração serão adicionados aqui.
    }
}