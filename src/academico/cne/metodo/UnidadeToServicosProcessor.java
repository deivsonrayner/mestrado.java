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

	public static void main(String[] args) throws IOException, URISyntaxException {
		// TODO Auto-generated method stub
		
		String censoGeoShapeDir = "C:\\projetos\\mestrado\\dados\\geojson-setores\\all";
		String cnesGeoFile = "C:\\projetos\\mestrado\\dados\\final\\estabelecimentos.rm.distritofed.csv";
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
		
		Collection<Collection> particoes = particionar(setores.values(), 1000000000); 
		int particaoIdx = 0;
		
		for (Collection<Setor> particao : particoes) {
			final int finalParticao = particaoIdx++;
			
			Thread t = new Thread( new Runnable() {
				
				@Override
				public void run() {
					
					HashMap<String,Relacionamento> relacionamentos = UnidadeToServicosProcessor.processarRelacionamentosMenorDistTolerancia(particao, estabelecimentos, servicos, classificadores, inputDistanciaInicial, inputPercToleranciaDist, inputLayer);
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
	 */
	
	public static HashMap<String,Relacionamento> processarRelacionamentosMenorDistTolerancia(Collection<Setor> setores, Collection<Estabelecimento> estabelecimentos, Map<String, Collection<Servico>> servicos, Collection<String> classificadores, int distanciaInicial, double PercToleranciaDist, String layer) {
		
		HashMap<String,Relacionamento> relacionamentos = new HashMap<String, Relacionamento>();
		
		int interacoes = setores.size() * classificadores.size();
		int count = 0;
		
		for (Setor setor : setores) {
						
			double menorDistancia = 1000000000;
			boolean continuaProcura = true;
			double raioPesquisa = distanciaInicial;
			
			for (String classificadorItem : classificadores) { 
				
				String servicoProcurado = classificadorItem.split("[.]")[0];
				String classificadorProcurado = classificadorItem.split("[.]")[1];
				
				do {	
					boolean achouMenorDistancia = false;
					boolean expandiuRaio = false;
					
					for (Estabelecimento estabelecimento : estabelecimentos) {
						
						Collection<Servico> servicosEstabelecimento = servicos.get(estabelecimento.cnes);
						
						Servico servicoOferecido = null;
						
						for (Servico servico: servicosEstabelecimento) {
							
							if (classificadorProcurado.equalsIgnoreCase("X")) {
								if (servico.codigo == servicoProcurado) {
									servicoOferecido = servico;
									break;
								}
							} else {
								if (servico.codigo == servicoProcurado && servico.classe == classificadorProcurado) {
									servicoOferecido = servico;
									break;
								}
							}
						}
						
						if (servicoOferecido == null) {
							continue;
						}
				
						
						boolean match = false;
						double distance = GeoGeometry.distance(estabelecimento.latitude, estabelecimento.longitude, setor.centroMassLat, setor.centroMassLng);
						
						
						match = distance < raioPesquisa;
						menorDistancia = Math.min(distance, menorDistancia);
						expandiuRaio = raioPesquisa != distanciaInicial;
						
						if (!achouMenorDistancia && (menorDistancia == distance)) {
							achouMenorDistancia = true;
						}
						
						if (match) {
							
							Relacionamento relacionamento = new Relacionamento();
							
							relacionamento.id = setor.id+"-"+estabelecimento.id;
							
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
							
							relacionamentos.put(relacionamento.id,relacionamento);
							
							System.out.println("[ACEITANDO] SETOR: " + setor.id
									+ " RELACIONADO COM: " + estabelecimento.id + " DISTANCIA: " + distance);
							
						} else {
							System.out.println("[IGNORANDO] SETOR: " + setor.id
									+ " RELACIONADO COM: " + estabelecimento.id + " DISTANCIA: " + distance);
						}
						
						
					}
					
					raioPesquisa = (int)(menorDistancia + (menorDistancia * 0.20));
					continuaProcura = !achouMenorDistancia || !expandiuRaio;
					
				} while (continuaProcura);
			}
			count++;
			System.out.println("[EVOLUCAO] - "+interacoes+" / "+count);
				
		}
		return relacionamentos;
	}
	
}
