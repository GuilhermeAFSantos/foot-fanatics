package br.senac.footfanatics;

import org.springframework.boot.SpringApplication;

public class TestFootFanaticsApplication {

	public static void main(String[] args) {
		SpringApplication.from(FootFanaticsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
