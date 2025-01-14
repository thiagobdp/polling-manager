package br.com.gerenciador.assembleias.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import br.com.gerenciador.assembleias.controller.form.AbreSessaoForm;
import br.com.gerenciador.assembleias.controller.form.PautaForm;

@Entity
public class Pauta {

	private static String TOPICO_NOVO_RESULTADO_VOTACAO = "NOVO_RESULTADO_VOTACAO";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@NotBlank
	@Size(min = 5)
	private String titulo;
	private String descricao;

	private LocalDateTime inicioSessao;
	private LocalDateTime fimSessao;

	private Integer qtdVotosSim = 0;
	private Integer qtdVotosNao = 0;

	// usar para verificar as sessoes que precisam ser analisadas
	@NotNull
	private Boolean sessaoFechada = false;

	@OneToMany(mappedBy = "pauta")
	private List<Voto> votos;

	public Pauta(@Valid PautaForm pautaForm) {
		this.titulo = pautaForm.getTitulo();
		this.descricao = pautaForm.getDescricao();
	}

	public Pauta() {
	}

	public Long getId() {
		return id;
	}

	public String getTitulo() {
		return titulo;
	}

	public String getDescricao() {
		return descricao;
	}

	public LocalDateTime getInicioSessao() {
		return inicioSessao;
	}

	public LocalDateTime getFimSessao() {
		return fimSessao;
	}

	public Boolean getSessaoFechada() {
		this.verificaSeFechaSessao();
		return sessaoFechada;
	}

	public Integer getQtdVotosSim() {
		this.verificaSeFechaSessao();
		return qtdVotosSim;
	}

	public Integer getQtdVotosNao() {
		this.verificaSeFechaSessao();
		return qtdVotosNao;
	}

	public List<Voto> getVotos() {
		return votos;
	}

	/**
	 * Abre a sessão atribuindo a hora de início e fim
	 * 
	 * @param abreSessaoForm
	 */
	public void abreSessao(AbreSessaoForm abreSessaoForm) {
		this.inicioSessao = LocalDateTime.now();
		LocalDateTime fimSessao = this.inicioSessao;
		Boolean usuarioInformouTempoSessao = false;

		if (abreSessaoForm != null) {
			if (abreSessaoForm.getDuracaoEmMinutos() != null
					&& abreSessaoForm.getDuracaoEmMinutos().compareTo(0L) > 0) {
				fimSessao = fimSessao.plusMinutes(abreSessaoForm.getDuracaoEmMinutos());
				usuarioInformouTempoSessao = true;
			}
			if (abreSessaoForm.getDuracaoEmHoras() != null && abreSessaoForm.getDuracaoEmHoras().compareTo(0L) > 0) {
				fimSessao = fimSessao.plusHours(abreSessaoForm.getDuracaoEmHoras());
				usuarioInformouTempoSessao = true;
			}
		}

		if (usuarioInformouTempoSessao) {
			this.fimSessao = fimSessao;
		} else {
			this.fimSessao = fimSessao.plusMinutes(1);
		}
	}

	public Boolean isSessaoIniciada() {
		return this.inicioSessao == null ? false : true;
	}

	/**
	 * Verifica se deve fechar a sessão. Caso esteja aberta, porém já tenha passado
	 * o horário de fechamento, realiza o fechamento e calcula o resultado dos
	 * votos.
	 */
	private void verificaSeFechaSessao() {
		if (!this.sessaoFechada) {
			if (this.fimSessao != null && LocalDateTime.now().isAfter(this.fimSessao)) {
				this.sessaoFechada = true;
				this.contabilizaVotos();
				this.enviaMensagemNovoResultadoVotacao();
			}
		}
	}

	/**
	 * Soma os votos e armazena o resultado final. Desta forma realiza a soma
	 * somente uma vez e armazena a contagem nas propriedades.
	 */
	private void contabilizaVotos() {
		if (this.qtdVotosSim.compareTo(0) == 0 && this.qtdVotosSim.compareTo(0) == 0) {
			this.votos.stream().forEach(v -> {
				if (v.getVoto().compareTo(VotoEnum.SIM) == 0) {
					++this.qtdVotosSim;
				} else {
					++this.qtdVotosNao;
				}
			});
		}
	}

	/**
	 * Envia mensagem com o resultado para o Apache Kafka
	 */
	private void enviaMensagemNovoResultadoVotacao() {
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(this.properties());
		String valores = this.id + "," + this.titulo + "," + this.verificaResultadoParaMensagem();
		String chaves = "id,titulo,resultado";
		ProducerRecord<String, String> record = new ProducerRecord<String, String>(Pauta.TOPICO_NOVO_RESULTADO_VOTACAO,
				chaves, valores);
		producer.send(record);
	}

	private ResultadoVotacaoEnum verificaResultadoParaMensagem() {
		if (this.qtdVotosSim == this.qtdVotosNao) {
			return ResultadoVotacaoEnum.EMPATE;
		} else if (this.qtdVotosSim > this.qtdVotosNao) {
			return ResultadoVotacaoEnum.APROVADO;
		} else {
			return ResultadoVotacaoEnum.REJEITADO;
		}
	}

	/**
	 * Propriedades para o Apache Kafka
	 * 
	 * @return
	 */
	private Properties properties() {
		Properties properties = new Properties();
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		return properties;
	}
}
