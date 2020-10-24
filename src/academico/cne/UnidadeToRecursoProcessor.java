package academico.cne;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.jillesvangurp.geo.GeoGeometry;

/**
 * Este processador é responsável por procurar recursos próximos de unidades.
 * Basicamente o processador procurará a únidade mais próxima que possui o
 * recurso, se a própria unidade origem oferecer este recurso um
 * auto-relacionamento será criado.
 * 
 * @author Deivson
 *
 */

public class UnidadeToRecursoProcessor extends AbstractGeoProcessor {

	public static Collection<Relacionamento> relacionamentos = new ArrayList<Relacionamento>();
	
	
	public static void gerarArquivostoGephi(String outPut) throws IOException {
		
		
		BufferedWriter tWriterVizinhos = new BufferedWriter(new FileWriter(outPut+"-relacionamentos.csv"));
		
		String line = "id,Source,Target,distancia,type";
		tWriterVizinhos.write(line);
		
		
		for (Relacionamento relacionamento : relacionamentos) {
			tWriterVizinhos.newLine();
			line = relacionamento.id+","+relacionamento.source+","+relacionamento.target+","+relacionamento.distancia+","+relacionamento.type;
			tWriterVizinhos.write(line);
		}
		tWriterVizinhos.flush();
		tWriterVizinhos.close();
	}

	public static void main(String[] args) throws IOException {

		String inputRegiao = "MG";
		int inputDistanciaInicial = 300;
		int inputDistanciaAdicional = 0;
		int inputNumMinVizinhos = 1;

		String origemUnidades = "C:\\projetos\\mestrado\\dados\\final\\everton\\CNES-MG-12-17-GEO-Everton-.csv";
		String origemRecurso = "C:\\projetos\\mestrado\\dados\\final\\everton\\";
		String toGephi = "C:\\projetos\\mestrado\\dados\\final\\everton\\relacionamentos-to-gephi.csv";
		String[] codServicos = new String[] { "116" };
		String[] codEquipamentos = new String[] { "41" };
		String[] tipUnidadeOrigem = new String[] { "1", "2" };
		String[] tipUnidadeDestino = new String[] {};


		carregarEstabelecimentosCNES(origemUnidades, inputRegiao, null);

		if (codEquipamentos.length > 0)
			//carregarEquipamentos(origemRecurso + "CNE-EQ-MG-12-17.csv", inputRegiao, null, codEquipamentos);

		if (codServicos.length > 0)
			carregarServicos(origemRecurso + "CNE-SR-MG-12-17.csv", inputRegiao, null, codServicos);

		System.out.println(estabelecimentos.size());
		System.out.println(equipamentos.size());
		System.out.println(servicos.size());
		
		processarRelacionamento(codEquipamentos, codServicos, tipUnidadeOrigem, tipUnidadeDestino, inputDistanciaInicial, inputDistanciaAdicional, inputNumMinVizinhos);
		gerarArquivostoGephi(toGephi);

	}

	public static void processarRelacionamento(String[] codEquipamentos, String[] codServicos,
			String[] tipUnidadeOrigem, String[] tipUnidadeDestino, int distanciaInicial, int distanciaAdicional,
			int numMinVizinhos) {

		

		for (Estabelecimento estabelecimentoOrigem : estabelecimentos) {
			
			if (estabelecimentoOrigem.latitude == -1) {
				continue;
			}
			
			Map<String, Double> processados = new HashMap<String, Double>();
			double menorDistancia = 1000000000;
			int numVizinhos = 0;
			int raioPesquisa = distanciaInicial;
			
			if (verificaOcorrencia(String.valueOf(estabelecimentoOrigem.tipUnidade), tipUnidadeOrigem)) {
				
				while (numVizinhos < numMinVizinhos) {
					
					for (Estabelecimento estabelecimentoDestino : estabelecimentos) {
						
						if (estabelecimentoDestino.latitude == -1) {
							continue;
						}

						if (verificaOcorrencia(String.valueOf(estabelecimentoDestino.tipUnidade), tipUnidadeDestino)) {

							if (verificaOcorrenciaServico(codServicos, estabelecimentoDestino.cnes) != true) {
								continue;
							}

							if (verificaOcorrenciaEquipamento(codEquipamentos, estabelecimentoDestino.cnes) != true) {
								continue;
							}

							double distance = GeoGeometry.distance(estabelecimentoOrigem.latitude,
									estabelecimentoOrigem.longitude, estabelecimentoDestino.latitude,
									estabelecimentoDestino.longitude);

							if (distance < menorDistancia) {
								menorDistancia = distance;
							}

							if (distance <= raioPesquisa) {

								Relacionamento relacionamento = new Relacionamento();
								relacionamento.id = estabelecimentoOrigem.cnes + "-" + estabelecimentoDestino.cnes;
								relacionamento.source = estabelecimentoOrigem.cnes;
								relacionamento.target = estabelecimentoDestino.cnes;
								relacionamento.distancia = distance;
								relacionamento.type = "ESTABELECIMENTO-TO-ESTABELECIMENTO";

								if (!processados.containsKey(relacionamento.id)) {
									relacionamentos.add(relacionamento);
									processados.put(relacionamento.id, relacionamento.distancia);
									numVizinhos++;
									System.out.println("[ACEITANDO] ESTABELECIMENTO: " + estabelecimentoOrigem.id
											+ " COM VIZINHO: " + estabelecimentoDestino.id + " A RAIO: " + raioPesquisa
											+ " DISTANCIA: " + distance);
								}

							} else {
								System.out.println("[IGNORANDO] ESTABELECIMENTO: " + estabelecimentoOrigem.id
										+ " COM VIZINHO: " + estabelecimentoDestino.id + " A RAIO: " + raioPesquisa
										+ " DISTANCIA: " + distance);
							}

						}
					}

					if (numVizinhos > 0)
						System.out.println("[FIM] ESTABELECIMENTO: " + estabelecimentoOrigem.id + " COM VIZINHOS: "
								+ numVizinhos + " A RAIO: " + raioPesquisa);
					
					if (distanciaAdicional > 0)
						raioPesquisa += distanciaAdicional;
					else 
						raioPesquisa = (int)(menorDistancia + (menorDistancia * 0.20));

				}
			}
		}

	}

	/**
	 * Verifica se um determinado CNES possui um determinado conjunto de servicos
	 * 
	 * @param colServicos
	 * @param cnes
	 * @return
	 */
	public static boolean verificaOcorrenciaServico(String[] colServicos, String cnes) {
		if (colServicos.length == 0) {
			return true;
		}
		for (Servico servico : servicos) {
			if (servico.cnes.equalsIgnoreCase(cnes)) {
				if (verificaOcorrencia(servico.codigo, colServicos)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Verifica se um determinado CNES possui um determinado conjunto de
	 * equipamentos
	 * 
	 * @param colEquipamentos
	 * @param cnes
	 * @return
	 */
	public static boolean verificaOcorrenciaEquipamento(String[] colEquipamentos, String cnes) {
		if (colEquipamentos.length == 0) {
			return true;
		}
		for (Equipamento equipamento : equipamentos) {
			if (equipamento.cnes.equalsIgnoreCase(cnes)) {
				if (verificaOcorrencia(equipamento.codigo, colEquipamentos)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean verificaOcorrencia(String valor, String[] colecao) {
		if (colecao.length == 0) {
			return true;
		}
		for (String item : colecao) {
			if (item.equalsIgnoreCase(valor)) {
				return true;
			}
		}
		return false;
	}

}
