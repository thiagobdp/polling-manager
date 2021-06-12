package br.com.gerenciador.assembleias.controller.dto;

import java.time.LocalDateTime;

import br.com.gerenciador.assembleias.model.Pauta;

public class SessaoAbertaDto {

	private Long id;
	private LocalDateTime inicioSessao;
	private LocalDateTime fimSessao;
	
	public static SessaoAbertaDto converter(Pauta pauta) {
		SessaoAbertaDto sessaoAberta = new SessaoAbertaDto();
		sessaoAberta.setId(pauta.getId());
		sessaoAberta.setInicioSessao(pauta.getInicioSessao());
		sessaoAberta.setFimSessao(pauta.getFimSessao());
		return sessaoAberta;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDateTime getInicioSessao() {
		return inicioSessao;
	}

	public void setInicioSessao(LocalDateTime inicio) {
		this.inicioSessao = inicio;
	}

	public LocalDateTime getFimSessao() {
		return fimSessao;
	}

	public void setFimSessao(LocalDateTime fim) {
		this.fimSessao = fim;
	}

	

}
