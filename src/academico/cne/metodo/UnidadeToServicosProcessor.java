package academico.cne.metodo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.jillesvangurp.geo.GeoGeometry;

import academico.cne.AbstractGeoProcessor;
import academico.cne.Estabelecimento;
import academico.cne.Relacionamento;
import academico.cne.Servico;
import academico.cne.Setor;

public class UnidadeToServicosProcessor extends AbstractGeoProcessor {
	
	public static BufferedWriter tWritterMatch = null;
	public static BufferedWriter tWritterAccepting = null;
	public static BufferedWriter tWritterNotFound = null;
	public static int interacoes = 0;
	public static int count = 0;
	
	public static void gerarArquivostoGephi(String outPut, HashMap<String,Relacionamento> relacionamentos) throws IOException {
		
		
		BufferedWriter tWriterVizinhos = new BufferedWriter(new FileWriter(outPut+"-relacionamentos.csv"));
		
		String line = "id,Source,Target,distancia,type,subtype,classifier,layer";
		tWriterVizinhos.write(line);
		
		
		for (Relacionamento relacionamento : relacionamentos.values()) {
			tWriterVizinhos.newLine();
			line = relacionamento.id+","+relacionamento.source+","+relacionamento.target+","+relacionamento.distancia+","+relacionamento.type+","+relacionamento.subType+","+relacionamento.classifier+","+relacionamento.layer;
			tWriterVizinhos.write(line);
		}
		tWriterVizinhos.flush();
		tWriterVizinhos.close();
	}
	
	public static void gerarSetoresAsGephiNodes(String outPut, HashMap<String,Setor> setores) throws IOException {
		
		
		BufferedWriter tWriterVizinhos = new BufferedWriter(new FileWriter(outPut+"-setores.csv"));
		
		String line = "id,ibge,regiao,latitude,longitude";
		tWriterVizinhos.write(line);
		
		
		for (Setor setor : setores.values()) {
			tWriterVizinhos.newLine();
			line = setor.id+","+setor.ibge+","+setor.regiao+","+setor.centroMassLat+","+setor.centroMassLng+","+"SETOR-CENSITARIO";
			tWriterVizinhos.write(line);
		}
		tWriterVizinhos.flush();
		tWriterVizinhos.close();
	}
	
	
	public static void traceMatch(String line) throws IOException {
		
		if (tWritterMatch == null)
			tWritterMatch = new BufferedWriter(new FileWriter("C:\\projetos\\mestrado\\logs\\match.log"));
		tWritterMatch.write(line+"\n");
		tWritterMatch.flush();
	}
	
	public static void traceAccepting(String line) throws IOException {
		
		if (tWritterAccepting == null)
			tWritterAccepting = new BufferedWriter(new FileWriter("C:\\projetos\\mestrado\\logs\\accepting.log"));
		tWritterAccepting.write(line+"\n");
		tWritterAccepting.flush();
	}
	
	public static void traceNotFound(String line) throws IOException {
		
		if (tWritterNotFound == null)
			tWritterNotFound = new BufferedWriter(new FileWriter("C:\\projetos\\mestrado\\logs\\notfound.log"));
		tWritterNotFound.write(line+"\n");
		tWritterNotFound.flush();
	}

	public static void main(String[] args) throws IOException, URISyntaxException {
		// TODO Auto-generated method stub
		
		String censoGeoShapeDir = "C:\\projetos\\mestrado\\dados\\geojson-setores\\all";
		String cnesGeoFile = "C:\\projetos\\mestrado\\r-projeto\\mestrado-r\\metodo\\cnes.resultado-filtro-unidades-servicos.csv";
		String cnesServicosFile = "C:\\projetos\\mestrado\\dados\\cnes-processado\\CNES-SR-12-18-METODO.csv";
		
		String[] ibges = new String[] {"310930","310945","317040","310450","520010","520017",
				                       "520025","520030","520400","520549","520551","520580",
				                       "520620","520800","521250","521305","521523","521560", 
				                       "521730","521760","521975","522185","522220","530010",
				                       "520060","520080","520320","520530","520790","520860", 
				                       "521460","522000","522068","522230"};
		
		
		String[] codigos = new String[] {"ALL"};
		
		String[] ufs = new String[] {};
		String[] inputRegiao = new String[] {"MG_NORTE_NORDESTE","MG_LESTE","GO","DF"};
		
		int inputDistanciaInicial = 300;
		double inputPercToleranciaDist = 0.2;
		String inputLayer = "ACESSO-POTENCIAL";
		
		long time = System.currentTimeMillis();
		Collection<String> classificadores = UnidadeToServicosProcessor.carregarClassificadoresPesquisados();
		System.out.println("[LOADING DATA] - Classificadores carregados em: " + (System.currentTimeMillis() - time) + " (ms)");
		
		time = System.currentTimeMillis();
		Collection<Estabelecimento> estabelecimentos = AbstractGeoProcessor.carregarEstabelecimentosMetodo(cnesGeoFile);
		System.out.println("[LOADING DATA] - Estabelecimentos carregados em: " + (System.currentTimeMillis() - time) + " (ms)");
		
		time = System.currentTimeMillis();
		Map<String, Setor> setores = AbstractGeoProcessor.carregarSetoresMetodo(censoGeoShapeDir, inputRegiao, ibges);
		System.out.println("[LOADING DATA] - Setores carregados em: " + (System.currentTimeMillis() - time) + " (ms)");
		
		time = System.currentTimeMillis();
		Map<String, Collection<Servico>> servicos = AbstractGeoProcessor.carregarServicosMetodo(cnesServicosFile, ufs, ibges, codigos);
		System.out.println("[LOADING DATA] - Servicos carregados em: " + (System.currentTimeMillis() - time) + " (ms)");
		
		Collection<Collection> particoes = particionar(setores.values(), 1000); 
		int particaoIdx = 0;
		interacoes = setores.size() * classificadores.size();
		
		for (Collection<Setor> particao : particoes) {
			final int finalParticao = particaoIdx++;
			
			Thread t = new Thread( new Runnable() {
				
				@Override
				public void run() {
					
					HashMap<String, Relacionamento> relacionamentos = null;
					try {
						relacionamentos = UnidadeToServicosProcessor.processarRelacionamentosMenorDistTolerancia(particao, estabelecimentos, servicos, classificadores, inputDistanciaInicial, inputPercToleranciaDist, inputLayer, finalParticao);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					try {
						UnidadeToServicosProcessor.gerarArquivostoGephi("C:\\projetos\\mestrado\\dados\\final\\metodo-relacionamentos_PARTICAO_"+finalParticao, relacionamentos);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			});
			
			t.start();
		}
		
	}
	
	private static Collection<String> carregarClassificadoresPesquisados() throws IOException, URISyntaxException {
		Collection<String> classificadores = new ArrayList<String>();
		URL classificadoresURL = ClassLoader.getSystemResource("parametro-rede-classificadores.csv");
		classificadores = FileUtils.readLines(new File(classificadoresURL.toURI()));
		return classificadores;
	}

	
	private static Collection<Collection> particionar(Collection collection, int particoes) {
		Collection<Collection> retorno = new ArrayList<Collection>();
		Collection innerCollection = new ArrayList();
		
		for (Object obj : collection) {
			innerCollection.add(obj);
			
			if ((innerCollection.size() % particoes) == 0) {
				retorno.add(innerCollection);
				innerCollection = new ArrayList();
			}
		}
		
		if (innerCollection.size() > 0) {
			retorno.add(innerCollection);
		}
		
		return retorno;
	}
	
		
	/**
	 * 
	 * @param estabelecimentos
	 * @param distanciaInicial
	 * @param numMinVizinhos
	 * @param distanciaAdicional
	 * @param PercToleranciaDist 
	 * @param tipoDistancia (1 - Vizinhos dentro de um Raio / 2 - O mais próximo centro de massa / 3 - Insideness)
	 * @throws IOException 
	 */
	
	public static HashMap<String,Relacionamento> processarRelacionamentosMenorDistTolerancia(Collection<Setor> setores, Collection<Estabelecimento> estabelecimentos, Map<String, Collection<Servico>> servicos, Collection<String> classificadores, int distanciaInicial, double PercToleranciaDist, String layer, int particao) throws IOException {
		
		HashMap<String,Relacionamento> relacionamentos = new HashMap<String, Relacionamento>();
		
		
		
		for (Setor setor : setores) {
						
			
			
			for (String classificadorItem : classificadores) { 
				
				String servicoProcurado = classificadorItem.split("[.]")[0];
				String classificadorProcurado = classificadorItem.split("[.]")[1];
				Collection<Estabelecimento> unidadesSelecionadas = new ArrayList<Estabelecimento>();
				Servico servicoOferecido = null;
				
				double menorDistancia = 1000000000;
				boolean continuaProcura = true;
				double raioPesquisa = distanciaInicial;
				
				for (Estabelecimento estabelecimento : estabelecimentos) {
					Collection<Servico> servicosEstabelecimento = servicos.get(estabelecimento.cnes);
					
					
					
					for (Servico servico: servicosEstabelecimento) {
						
						if (classificadorProcurado.equalsIgnoreCase("X")) {
							if (servico.codigo.equalsIgnoreCase(servicoProcurado)) {
								servicoOferecido = servico;
								break;
							}
						} else {
							if (servico.codigo.equalsIgnoreCase(servicoProcurado) && servico.classe.equalsIgnoreCase(classificadorProcurado)) {
								servicoOferecido = servico;
								break;
							}
						}
					}
					
					if (servicoOferecido != null) {
//						System.out.println("[Relacionamento] - SERVICO ENCONTRADO: "+ servicoProcurado +"."+classificadorProcurado + " no CNES: "+estabelecimento.cnes + " DESCRICAO: "+estabelecimento.descricao + " SERVICOS: "+estabelecimento.servicos);
						unidadesSelecionadas.add(estabelecimento);
					}
				}
				if (unidadesSelecionadas.size() > 0) {
					boolean expandiuRaio = false;
					do {	

						for (Estabelecimento estabelecimento : unidadesSelecionadas) {
							
							boolean match = false;
							double distance = GeoGeometry.distance(estabelecimento.latitude, estabelecimento.longitude, setor.centroMassLat, setor.centroMassLng);
													
							match = distance < raioPesquisa;
							menorDistancia = Math.min(distance, menorDistancia);
							
							
							if (match) {
								
								Relacionamento relacionamento = new Relacionamento();
								traceMatch("[Relacionamento - PART:"+particao+"]  - MATCH: "+ servicoProcurado +"."+classificadorProcurado + " no CNES: "+estabelecimento.cnes + " DESCRICAO: "+estabelecimento.descricao + " DISTANCIA: "+distance + " MENOR DISTANCIA: "+ menorDistancia + " RAIO PESQUISA: "+raioPesquisa);
								
								
								relacionamento.source = setor.id;
								relacionamento.target = estabelecimento.id;
								relacionamento.distancia = distance;
								relacionamento.type = "SETOR-TO-ESTABELECIMENTO-SERVICO";
								relacionamento.subType = servicoOferecido.codigo +" - "+ servicoOferecido.getDescricao();
								
								if (classificadorProcurado.equalsIgnoreCase("X")) {
									relacionamento.classifier = "*";
								} else {
									relacionamento.classifier = servicoOferecido.classe + " - " + servicoOferecido.getTipoDescricao();
								}
								
								relacionamento.layer = layer;
								relacionamento.id = setor.id+"-"+estabelecimento.id+"-"+servicoOferecido.codigo+"-"+relacionamento.classifier;
								relacionamentos.put(relacionamento.id,relacionamento);
								
								traceAccepting("[ACEITANDO - PART:"+particao+"] SETOR: " + setor.id
										+ " RELACIONADO COM: " + estabelecimento.id + " DISTANCIA: " + distance + " SERVICO: "+servicoProcurado +"."+classificadorProcurado);
								
							} else {
								//System.out.println("[IGNORANDO] SETOR: " + setor.id
								//		+ " RELACIONADO COM: " + estabelecimento.id + " DISTANCIA: " + distance + "RAIO DE PESQUISA: "+ raioPesquisa + " MENOR DISTANCIA: "+menorDistancia);
							}
							
							
						}
						
						raioPesquisa = (int)(menorDistancia + (menorDistancia * 0.20));
						continuaProcura = !expandiuRaio;
						expandiuRaio = true;
						
					} while (continuaProcura);
				} else {
					traceNotFound("[NAO_ENCONTRADO - PART:"+particao+"] - SERVICO: "+servicoProcurado+"."+classificadorProcurado);
				}
				
			}
			count++;
			System.out.println("[EVOLUCAO PART:"+particao+"] - "+interacoes+" / "+count + "RELACIONAMENTOS: " + relacionamentos.size());
				
		}
		return relacionamentos;
	}
	
}
