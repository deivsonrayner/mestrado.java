package academico.cne;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;

import com.jillesvangurp.geo.GeoGeometry;

public class AbstractGeoProcessor {

	public static Map<String, Setor> setoresCache = new HashMap<String, Setor>();
	public static Collection<GeoObjeto> geoObjetos = new ArrayList<GeoObjeto>();
	public static Collection<Estabelecimento> estabelecimentos = new ArrayList<Estabelecimento>();
	public static Collection<Servico> servicos = new ArrayList<Servico>();
	public static Collection<Equipamento> equipamentos = new ArrayList<Equipamento>();
	
	
	public static Map<String, Setor> carregarSetoresMetodo(String dirOrigem, String[] regioes, String[] ibges)
			throws IOException {
		File dir = new File(dirOrigem);
		Map<String, Setor> setores = new HashMap<String, Setor>();

		for (File subDir : dir.listFiles()) {
			boolean processar = false;
				
			for (String regiao : regioes) {
				if (subDir.getName().startsWith(regiao) || regiao.equalsIgnoreCase("ALL")) {
					processar = true;
					break;
				}
			}
			
			
			
			if (processar) {
				System.out.println("Processando Diretorio: " + subDir.getName());
				System.out.println("=================================================================");
				for (File file : subDir.listFiles()) {
					System.out.println("Avaliando Arquivo: " + file.getName());
	
					FileInputStream fileInput = new FileInputStream(file);
					byte[] content = new byte[(int) file.length()];
					fileInput.read(content);
	
					JSONArray json = new JSONArray(new String(content));
	
					for (int jIdx = 0; jIdx < json.length(); jIdx++) {
						// if
						// (!getIBGEAmostra(Integer.parseInt(json.getJSONObject(jIdx).getJSONObject("properties").getString("setor").substring(0,
						// 6)),regiao)) {
						// System.out.println("FORA DA AMOSTRA --> IBGE:
						// "+json.getJSONObject(jIdx).getJSONObject("properties").getString("setor").substring(0,
						// 6));
						// continue;
						// }
	
							JSONArray coordenadas = json.getJSONObject(jIdx).getJSONObject("geometry")
									.getJSONArray("coordinates").getJSONArray(0);
							Setor setor = new Setor();
	
							double[] ponto = PolygonUtilities
									.centerOfMass(PolygonUtilities.converteCoordenadasToPoint(coordenadas));
							setor.centroMassLng = ponto[0];
							setor.centroMassLat = ponto[1];
	
							double area = GeoGeometry.area(PolygonUtilities.converteCoordenadasToArray(coordenadas));
							setor.area = area;
							setor.geoJson = json.getJSONObject(jIdx).getJSONObject("geometry").toString();
	
							setor.regiao = subDir.getName();
							setor.ibge = json.getJSONObject(jIdx).getJSONObject("properties").getString("setor")
									.substring(0, 7);
							setor.ibge6 = json.getJSONObject(jIdx).getJSONObject("properties").getString("setor")
									.substring(0, 6);
							setor.id = json.getJSONObject(jIdx).getJSONObject("properties").getString("setor");
							setor.coordenadas = PolygonUtilities.converteCoordenadasToPoint(coordenadas);
							
							boolean inserirIBGE = false;
							
							for (String ibge : ibges) {
								if (ibge.equalsIgnoreCase(setor.ibge6) || ibge.equalsIgnoreCase("ALL")) {
									inserirIBGE = true;
									break;
								}
							}
							
							if (inserirIBGE) {
								setores.put(setor.id, setor);
								//System.out.println("[SETOR] INSERTING SETOR: " + setor.id + " IBGE: " + setor.ibge6
								//		+ " REGIAO: " + setor.regiao);
							}
						
					}
	
				}
			}

		}
		return setores;
	}


	public static void carregarSetores(String dirOrigem, String regiao) throws IOException {
		File dir = new File(dirOrigem);
		
		for (File subDir : dir.listFiles()) {
			
			System.out.println("Processando Diretorio: " + subDir.getName());
			System.out.println("=================================================================");
			for (File file : subDir.listFiles()) {
				System.out.println("Avaliando Arquivo: " + file.getName());
				
				FileInputStream fileInput = new FileInputStream(file);
				byte[] content = new byte[(int) file.length()];
				fileInput.read(content);
				
				JSONArray json = new JSONArray(new String(content));
				
				for (int jIdx = 0; jIdx < json.length(); jIdx++) {
					//if (!getIBGEAmostra(Integer.parseInt(json.getJSONObject(jIdx).getJSONObject("properties").getString("setor").substring(0, 6)),regiao)) {
					//	System.out.println("FORA DA AMOSTRA -->  IBGE: "+json.getJSONObject(jIdx).getJSONObject("properties").getString("setor").substring(0, 6));
					//	continue;
					//}
					
					if (subDir.getName().startsWith(regiao.toUpperCase()) || regiao.equalsIgnoreCase("ALL")) {
						JSONArray coordenadas = json.getJSONObject(jIdx).getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0);
						Setor setor = new Setor();
						
						double[] ponto = PolygonUtilities.centerOfMass(PolygonUtilities.converteCoordenadasToPoint(coordenadas));
						setor.centroMassLng = ponto[0];
						setor.centroMassLat = ponto[1];
						setor.geoJson = json.getJSONObject(jIdx).getJSONObject("geometry").toString();
						
						double area = GeoGeometry.area(PolygonUtilities.converteCoordenadasToArray(coordenadas));
						setor.area = area;
						
						setor.regiao = subDir.getName();
						setor.ibge = json.getJSONObject(jIdx).getJSONObject("properties").getString("setor").substring(0, 7);
						setor.ibge6 = json.getJSONObject(jIdx).getJSONObject("properties").getString("setor").substring(0, 6);
						setor.id =  json.getJSONObject(jIdx).getJSONObject("properties").getString("setor");
						setor.coordenadas = PolygonUtilities.converteCoordenadasToPoint(coordenadas);
						
						setoresCache.put(setor.id, setor);
						System.out.println("[SETOR] INSERTING SETOR: "+setor.id + " IBGE: "+setor.ibge6+ " REGIAO: "+setor.regiao);
					}
					
				}
					
			}
			
		}
	}

	public static void carregarObjetos(String dirObjetos) throws IOException {
			File file = new File(dirObjetos);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			boolean firstLine = true;
			while ((line = reader.readLine()) != null) {
				if (firstLine) {
					firstLine = !firstLine;
					continue;
				}
				
				String[] lineSplit = line.split(",");
				if (lineSplit[1].equalsIgnoreCase("NA")) {
					continue;
				}
				GeoObjeto geo = new GeoObjeto();
				geo.id = lineSplit[0];
				geo.lng = Double.parseDouble(lineSplit[1]);
				geo.lat = Double.parseDouble(lineSplit[2]);
				geoObjetos.add(geo);
			}
			
		
	}
	
	public static void carregarEquipamentos(String dirObjetos, String uf, String ibge, String[] codigos) throws NumberFormatException, IOException {
		File file = new File(dirObjetos);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		boolean firstLine = true;
		while ((line = reader.readLine()) != null) {
			if (firstLine) {
				firstLine = !firstLine;
				continue;
			}
			
			String[] lineSplit = line.split(",");
			if (lineSplit[1].equalsIgnoreCase("NA")) {
				continue;
			}
			
			if (uf != null) {
				if (!lineSplit[29].equalsIgnoreCase(uf)) {
					continue;
				}
			}
			
			if (ibge != null) {
				if (!ibge.equalsIgnoreCase(lineSplit[2])) {
					continue;
				}
			}
			
			Equipamento equipamento = new Equipamento();
			equipamento.cnes = lineSplit[1];
			equipamento.codigo = lineSplit[22];
			equipamento.tipo = lineSplit[21];
			equipamento.uf = lineSplit[29];
			equipamento.indDispSUS = lineSplit[25].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[25]);
			equipamento.intNDispSUS = lineSplit[26].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[26]);
			equipamento.qtdEmUso = lineSplit[24].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[24]);
			equipamento.qtdExistente = lineSplit[23].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[23]);

			if (uf != null) {
				if (!lineSplit[29].equalsIgnoreCase(uf)) {
					//System.out.println("[IGNORING...] - CNES: "+ equipamento.cnes + " UF: "+ equipamento.uf + " CODIGO: "+equipamento.codigo);
					continue;
				}
			}
			
			if (ibge != null) {
				if (!ibge.equalsIgnoreCase(lineSplit[2])) {
					//System.out.println("[IGNORING...] - CNES: "+ equipamento.cnes + " IBGE: "+ lineSplit[2] + " CODIGO: "+equipamento.codigo);
					continue;
				}
			}
			
			for (String codigo : codigos) {
				if (equipamento.codigo.equalsIgnoreCase(codigo)) {
					equipamentos.add(equipamento);
					System.out.println("[LOADING...] - CNES: "+ equipamento.cnes + " UF: "+ equipamento.uf + " CODIGO: "+equipamento.codigo);
					continue;
				}
			}
	
			//System.out.println("[IGNORING...] - CNES: "+ equipamento.cnes + " UF: "+ equipamento.uf + " CODIGO: "+equipamento.codigo);
			
		}
	}
	
	public static Map<String, Collection<Servico>> carregarServicosMetodo(String dirObjetos, String[] ufs, String[] ibges, String[] codigos) throws NumberFormatException, IOException {
		File file = new File(dirObjetos);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		boolean firstLine = true;
		Map<String, Collection<Servico>> servicos = new HashMap<String, Collection<Servico>>();
		
		while ((line = reader.readLine()) != null) {
			if (firstLine) {
				firstLine = !firstLine;
				continue;
			}
			
			String[] lineSplit = line.split(",");
			if (lineSplit[1].equalsIgnoreCase("NA")) {
				continue;
			}
			
			Servico servico = new Servico();
			servico.cnes = (lineSplit[1].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[1]))+"";
			servico.codigo = lineSplit[3];
			servico.classe = lineSplit[4];
			servico.uf = lineSplit[33];
			servico.ambNSUS = lineSplit[25].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[25]);
			servico.ambSUS = lineSplit[26].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[26]);
			servico.hospNSUS = lineSplit[27].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[27]);
			servico.hospSUS = lineSplit[28].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[28]);
			
			boolean processar = false;
			
			if (ufs == null || ufs.length == 0 || ufs[0].equalsIgnoreCase("ALL")) {
				if (ibges == null || ibges.length == 0) {
					processar = true;
				} else {
					for (String ibge : ibges) {
						if (lineSplit[2].startsWith(ibge) || ibge.equalsIgnoreCase("ALL")) {
							processar = true;
							break;
						} else {
							processar = false;
						}
					}
				}
			} else {
				for (String uf : ufs) {
					if (lineSplit[33].equalsIgnoreCase(uf)) {
						processar = true;
						break;
					}
				}
			}
			
			
			if (processar) {		
				for (String codigo : codigos) {
					if (servico.codigo.equalsIgnoreCase(codigo) || codigo.equalsIgnoreCase("ALL")) {
						
						if (!servicos.containsKey(servico.cnes)) {
							servicos.put(servico.cnes, new ArrayList<Servico>());
						}
						servicos.get(servico.cnes).add(servico);
						
						//System.out.println("[LOADING...] - CNES: "+ servico.cnes + " UF: "+ servico.uf + " CODIGO: "+servico.codigo);
						continue;
					}
				}
			} else {
				//System.out.println("[IGNORING...] - CNES: "+ servico.cnes + " UF: "+ servico.uf + " CODIGO: "+servico.codigo);
			}
		}
		
		return servicos;
	}
	
	public static void carregarServicos(String dirObjetos, String uf, String ibge, String[] codigos) throws NumberFormatException, IOException {
		File file = new File(dirObjetos);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		boolean firstLine = true;
		while ((line = reader.readLine()) != null) {
			if (firstLine) {
				firstLine = !firstLine;
				continue;
			}
			
			String[] lineSplit = line.split(",");
			if (lineSplit[1].equalsIgnoreCase("NA")) {
				continue;
			}
			

			

			
			Servico servico = new Servico();
			servico.cnes = lineSplit[1];
			servico.codigo = lineSplit[3];
			servico.classe = lineSplit[4];
			servico.uf = lineSplit[33];
			servico.ambNSUS = lineSplit[25].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[25]);
			servico.ambSUS = lineSplit[26].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[26]);
			servico.hospNSUS = lineSplit[27].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[27]);
			servico.hospSUS = lineSplit[28].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[28]);
			
			if (uf != null) {
				if (!lineSplit[33].equalsIgnoreCase(uf)) {
					//System.out.println("[IGNORING...] - CNES: "+ servico.cnes + " UF: "+ servico.uf + " CODIGO: "+servico.codigo);
					continue;
				}
			}
			
			if (ibge != null) {
				if (!ibge.equalsIgnoreCase(lineSplit[2])) {
					//System.out.println("[IGNORING...] - CNES: "+ servico.cnes + " IBGE: "+ lineSplit[2] + " CODIGO: "+servico.codigo);
					continue;
				}
			}
			
			for (String codigo : codigos) {
				if (servico.codigo.equalsIgnoreCase(codigo)) {
					servicos.add(servico);
					System.out.println("[LOADING...] - CNES: "+ servico.cnes + " UF: "+ servico.uf + " CODIGO: "+servico.codigo);
					continue;
				}
			}
	
			//System.out.println("[IGNORING...] - CNES: "+ servico.cnes + " UF: "+ servico.uf + " CODIGO: "+servico.codigo);

		}
	}
	
	public static Collection<EstabelecimentoLocal> carregarEstabelecimentoLocalSimples(String dirObjetos) throws IOException {
		File file = new File(dirObjetos);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		boolean firstLine = true;
		
		ArrayList<EstabelecimentoLocal> locais = new ArrayList<EstabelecimentoLocal>();
		
		while ((line = reader.readLine()) != null) {
			if (firstLine) {
				firstLine = !firstLine;
				continue;
			}
			
			String[] lineSplit = line.split(",");
			if (lineSplit[0].equalsIgnoreCase("NA")) {
				continue;
			}
			
			EstabelecimentoLocal estabelecimentoLocal = new EstabelecimentoLocal();
			estabelecimentoLocal.id = lineSplit[0].replace( "\"", "");
			estabelecimentoLocal.cnes = estabelecimentoLocal.id;
			estabelecimentoLocal.ibge = lineSplit[1].replace( "\"", "");
			estabelecimentoLocal.latitude  = Double.parseDouble(lineSplit[4].replace("\"", ""));
			estabelecimentoLocal.longitude = Double.parseDouble(lineSplit[5].replace("\"", ""));
			
			locais.add(estabelecimentoLocal);
		}
		
		return locais;
		
	}
	
	public static void carregarEstabelecimentosCNES(String dirObjetos, String uf, String ibge) throws IOException {
		File file = new File(dirObjetos);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		boolean firstLine = true;
		while ((line = reader.readLine()) != null) {
			if (firstLine) {
				firstLine = !firstLine;
				continue;
			}
			
			String[] lineSplit = line.split(",");
			if (lineSplit[1].equalsIgnoreCase("NA")) {
				continue;
			}
			
			if (uf != null) {
				if (!uf.equalsIgnoreCase(lineSplit[202])) {
					continue;
				}
			}
			
			if (ibge != null) {
				if (!ibge.equalsIgnoreCase(lineSplit[2])) {
					continue;
				}
			}
			
			Estabelecimento estabelecimento = new Estabelecimento();
			estabelecimento.tipUnidade = lineSplit[20].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[20]);
			estabelecimento.cnes = lineSplit[1];
			estabelecimento.id = lineSplit[1];
			estabelecimento.nivDependencia = lineSplit[6].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[6]);
			estabelecimento.vincSus = lineSplit[13].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[13]);
			estabelecimento.tpGestao = lineSplit[14];
			estabelecimento.atividade = lineSplit[17].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[17]);
			estabelecimento.clientel = lineSplit[19].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[19]);
			estabelecimento.turnoAt = lineSplit[21].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[21]);
			estabelecimento.atendAmbulatorial = lineSplit[88].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[88]);
			estabelecimento.centroNeoNatal = lineSplit[113].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[113]);
			estabelecimento.atendHospitalar = lineSplit[114].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[114]);
			estabelecimento.urgEmergencia = lineSplit[71].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[71]);
			estabelecimento.centroCirurgico = lineSplit[92].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[92]);
			estabelecimento.centroObstetrico = lineSplit[97].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[97]);
			estabelecimento.nivelAtendAmb = lineSplit[45].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[45]);
			estabelecimento.nivelAtendHos = lineSplit[52].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[52]);
			estabelecimento.atendPr = lineSplit[198].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[198]);
			estabelecimento.longitude = lineSplit[211].equalsIgnoreCase("NA")?-1:Double.parseDouble(lineSplit[211]);
			estabelecimento.latitude = lineSplit[212].equalsIgnoreCase("NA")?-1:Double.parseDouble(lineSplit[212]);
			estabelecimento.ano = lineSplit[203].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[203]);
			estabelecimento.uf = lineSplit[202];
			//estabelecimento.totalEquipes = lineSplit[24].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[24]);
			//estabelecimento.atencaoBasica = lineSplit[25].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[25]);
			//estabelecimento.mediaComplexidade = lineSplit[27].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[27]);
			//estabelecimento.altaComplexidade = lineSplit[28].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[28]);
	
			
			
			estabelecimentos.add(estabelecimento);
			System.out.println("[LOADING...] - CNES: "+ estabelecimento.cnes + " UF: "+ estabelecimento.uf);
		}
		
	
}

	public static Collection<Estabelecimento> carregarEstabelecimentosMetodo(String dirObjetos) throws IOException {
			File file = new File(dirObjetos);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			Collection<Estabelecimento> estabelecimentosRetorno = new ArrayList<Estabelecimento>();
			
			String line = null;
			boolean firstLine = true;
			while ((line = reader.readLine()) != null) {
				if (firstLine) {
					firstLine = !firstLine;
					continue;
				}
				
				String[] lineSplit = line.split(",");
				if (lineSplit[1].equalsIgnoreCase("NA")) {
					continue;
				}
				
				
				Estabelecimento estabelecimento = new Estabelecimento();
				estabelecimento.tipUnidade = lineSplit[2].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[2]);
				estabelecimento.cnes = lineSplit[1];
				estabelecimento.id = lineSplit[1];
				estabelecimento.nivDependencia = lineSplit[3].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[3]);
				estabelecimento.vincSus = lineSplit[4].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[3]);
				estabelecimento.tpGestao = lineSplit[5];
				estabelecimento.atividade = lineSplit[6].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[6]);
				estabelecimento.clientel = lineSplit[7].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[7]);
				estabelecimento.turnoAt = lineSplit[8].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[8]);
				estabelecimento.atendAmbulatorial = lineSplit[9].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[9]);
				estabelecimento.centroNeoNatal = lineSplit[10].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[10]);
				estabelecimento.atendHospitalar = lineSplit[11].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[11]);
				estabelecimento.urgEmergencia = lineSplit[12].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[12]);
				estabelecimento.centroCirurgico = lineSplit[13].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[13]);
				estabelecimento.centroObstetrico = lineSplit[14].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[14]);
				estabelecimento.nivelAtendAmb = lineSplit[15].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[15]);
				estabelecimento.nivelAtendHos = lineSplit[16].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[16]);
				estabelecimento.atendPr = lineSplit[17].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[17]);
				estabelecimento.longitude = lineSplit[25].equalsIgnoreCase("NA")?-1:Double.parseDouble(lineSplit[25]);
				estabelecimento.latitude = lineSplit[26].equalsIgnoreCase("NA")?-1:Double.parseDouble(lineSplit[26]);
				estabelecimento.ano = lineSplit[27].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[27]);
				estabelecimento.totalEquipes = lineSplit[28].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[28]);
				estabelecimento.atencaoBasica = lineSplit[29].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[29]);
				estabelecimento.mediaComplexidade = lineSplit[31].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[31]);
				estabelecimento.altaComplexidade = lineSplit[32].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[32]);
				estabelecimento.descricao = lineSplit[30];
				estabelecimento.servicos = lineSplit[35].equalsIgnoreCase("NA")?0:Integer.parseInt(lineSplit[35]);
				estabelecimento.uf = lineSplit[20];
				estabelecimento.municipio = lineSplit[36];
				
				estabelecimentosRetorno.add(estabelecimento);
			}
			
			return estabelecimentosRetorno;
			
		
	}
	
	public static void carregarEstabelecimentos(String dirObjetos) throws IOException {
		File file = new File(dirObjetos);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		boolean firstLine = true;
		while ((line = reader.readLine()) != null) {
			if (firstLine) {
				firstLine = !firstLine;
				continue;
			}
			
			String[] lineSplit = line.split(",");
			if (lineSplit[1].equalsIgnoreCase("NA")) {
				continue;
			}
			
			
			Estabelecimento estabelecimento = new Estabelecimento();
			estabelecimento.tipUnidade = lineSplit[0].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[0]);
			estabelecimento.cnes = lineSplit[1];
			estabelecimento.id = lineSplit[1];
			estabelecimento.nivDependencia = lineSplit[2].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[2]);
			estabelecimento.vincSus = lineSplit[3].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[3]);
			estabelecimento.tpGestao = lineSplit[4];
			estabelecimento.atividade = lineSplit[5].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[5]);
			estabelecimento.clientel = lineSplit[6].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[6]);
			estabelecimento.turnoAt = lineSplit[7].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[7]);
			estabelecimento.atendAmbulatorial = lineSplit[8].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[8]);
			estabelecimento.centroNeoNatal = lineSplit[9].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[9]);
			estabelecimento.atendHospitalar = lineSplit[10].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[10]);
			estabelecimento.urgEmergencia = lineSplit[11].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[11]);
			estabelecimento.centroCirurgico = lineSplit[12].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[12]);
			estabelecimento.centroObstetrico = lineSplit[13].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[13]);
			estabelecimento.nivelAtendAmb = lineSplit[14].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[14]);
			estabelecimento.nivelAtendHos = lineSplit[15].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[15]);
			estabelecimento.atendPr = lineSplit[16].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[16]);
			estabelecimento.longitude = lineSplit[21].equalsIgnoreCase("NA")?-1:Double.parseDouble(lineSplit[21]);
			estabelecimento.latitude = lineSplit[22].equalsIgnoreCase("NA")?-1:Double.parseDouble(lineSplit[22]);
			estabelecimento.ano = lineSplit[23].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[23]);
			estabelecimento.totalEquipes = lineSplit[24].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[24]);
			estabelecimento.atencaoBasica = lineSplit[25].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[25]);
			estabelecimento.mediaComplexidade = lineSplit[27].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[27]);
			estabelecimento.altaComplexidade = lineSplit[28].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[28]);
	
			
			
			estabelecimentos.add(estabelecimento);
		}
		
	
}

	public static boolean getIBGEAmostra(int ibge, String regiao) {
		int[] amostra = null;
		
		if (regiao == null) {
			amostra = new int[] {231000,230350,230370,230395,230440,230765,230770,230960,230970,231085,230428,230495,230523,230625,231240,
					310930,310945,317040,520010,520017,520025, 520030, 520400, 520549, 520551, 520580, 520620, 520800, 521250, 521305, 521523, 521560, 521730, 521760, 521975, 522185, 522220, 530010,
					410020,410030,410040,410180,410230,410310,410400,410410,410420,410425,410520,410580,410620,410690,412863,410765,411125,411320,411430,411915,411910,411950,412080,412220,412120,412230,412550,412760,412788};
		}
		
		if (regiao != null && regiao.equalsIgnoreCase("CE_METRO")) {
			amostra = new int[] {231000,230350,230370,230395,230440,230765,230770,230960,230970,231085,230428,230495,230523,230625,231240};
		}
		
		if (regiao != null && regiao.equalsIgnoreCase("DF_METRO")) {
			//amostra = new int[] {310930,310945,317040,520010,520017,520025, 520030, 520400, 520549, 520551, 520580, 520620, 520800, 521250, 521305, 521523, 521560, 521730, 521760, 521975, 522185, 522220, 530010};
			amostra = new int[] {530010};

		}
		
		if (regiao != null && regiao.equalsIgnoreCase("PR_METRO")) {
			amostra = new int[] {410020,410030,410040,410180,410230,410310,410400,410410,410420,410425,410520,410580,410620,410690,412863,410765,411125,411320,411430,411915,411910,411950,412080,412220,412120,412230,412550,412760,412788};
		}
		
			for (int item : amostra) {
				if (item == ibge) {
					return true;
				}
			}
		
		 
		
	
		return false;
	}

	public AbstractGeoProcessor() {
		super();
	}

}