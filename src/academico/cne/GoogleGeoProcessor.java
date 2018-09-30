package academico.cne;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.google.gson.Gson;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;
import com.google.maps.model.Unit;
import com.jillesvangurp.geo.GeoGeometry;

public class GoogleGeoProcessor extends AbstractGeoProcessor {

	public static final String API_KEY = "AIzaSyBmyh7v8Rq1_ELkaa-V7FY7QEiUq--YMWk";
	public static Collection<RelacionamentoSetor> relacionamentos = new ArrayList<RelacionamentoSetor>();
	public static Collection<RelacionamentoSetor> relacionamentosEuclidianos = new ArrayList<RelacionamentoSetor>();

	public static void calcularRotas(String regiao, String dirOutput)
			throws ApiException, InterruptedException, IOException {

		GeoApiContext geoContext = new GeoApiContext.Builder().apiKey(API_KEY).build();

		CloudantClient client = ClientBuilder.url(new URL("http://142.93.196.115:5984")).username("admin")
				.password("mestrado123").build();

		Database database = client.database("mestrado-geo-relationship-" + regiao.toLowerCase(), true);

		for (Setor setor : setoresCache.values()) {

			if (!getIBGEAmostra(Integer.parseInt(setor.ibge6), regiao)) {
				System.out.println("FORA DA AMOSTRA -->  IBGE: " + setor.ibge6);
				continue;
			}

			for (Estabelecimento estabelecimento : estabelecimentos) {
				String origin = setor.centroMassLat + "," + setor.centroMassLng;
				String destination = estabelecimento.latitude + "," + estabelecimento.longitude;

				JSONObject geoRelation = new JSONObject();
				geoRelation.put("setor", setor.id);
				geoRelation.put("estabelecimento_cnes", estabelecimento.cnes);
				geoRelation.put("estabelecimento_tipo", estabelecimento.tipUnidade);
				geoRelation.put("estabelecimento_atencaoBasica", estabelecimento.atencaoBasica);
				geoRelation.put("estabelecimento_atendAmbulatorial", estabelecimento.atendAmbulatorial);
				geoRelation.put("estabelecimento_atendHospitalar", estabelecimento.atendHospitalar);
				geoRelation.put("estabelecimento_altaComplexidade", estabelecimento.altaComplexidade);
				geoRelation.put("estabelecimento_mediaComplexidade", estabelecimento.mediaComplexidade);
				geoRelation.put("estabelecimento_atividade", estabelecimento.atividade);
				geoRelation.put("estabelecimento_centroCirurgico", estabelecimento.centroCirurgico);
				geoRelation.put("estabelecimento_centroNeoNatal", estabelecimento.centroNeoNatal);
				geoRelation.put("estabelecimento_centroObstetrico", estabelecimento.centroObstetrico);
				geoRelation.put("estabelecimento_clientel", estabelecimento.clientel);
				geoRelation.put("estabelecimento_totalEquipes", estabelecimento.totalEquipes);
				geoRelation.put("estabelecimento_turnoAt", estabelecimento.turnoAt);
				geoRelation.put("estabelecimento_urgEmergencia", estabelecimento.urgEmergencia);
				geoRelation.put("estabelecimento_vincSUS", estabelecimento.vincSus);

				JSONArray googleResults = new JSONArray();

				System.out.println("Processando SETOR: " + setor.id);

				DirectionsResult result = DirectionsApi.newRequest(geoContext).mode(TravelMode.DRIVING)
						// .avoid(
						// DirectionsApi.RouteRestriction.HIGHWAYS,
						// DirectionsApi.RouteRestriction.TOLLS,
						// DirectionsApi.RouteRestriction.FERRIES)
						.units(Unit.METRIC).origin(origin).destination(destination).await();

				googleResults.put(result);

				result = DirectionsApi.newRequest(geoContext).mode(TravelMode.BICYCLING)
						// .avoid(
						// DirectionsApi.RouteRestriction.HIGHWAYS,
						// DirectionsApi.RouteRestriction.TOLLS,
						// DirectionsApi.RouteRestriction.FERRIES)
						.units(Unit.METRIC).origin(origin).destination(destination).await();

				googleResults.put(result);

				result = DirectionsApi.newRequest(geoContext).mode(TravelMode.TRANSIT)
						// .avoid(
						// DirectionsApi.RouteRestriction.HIGHWAYS,
						// DirectionsApi.RouteRestriction.TOLLS,
						// DirectionsApi.RouteRestriction.FERRIES)
						.units(Unit.METRIC).origin(origin).destination(destination).await();

				googleResults.put(result);

				result = DirectionsApi.newRequest(geoContext).mode(TravelMode.WALKING)
						// .avoid(
						// DirectionsApi.RouteRestriction.HIGHWAYS,
						// DirectionsApi.RouteRestriction.TOLLS,
						// DirectionsApi.RouteRestriction.FERRIES)
						.units(Unit.METRIC).origin(origin).destination(destination).await();

				googleResults.put(result);
				geoRelation.put("resultados", googleResults);
				System.out.println("FIM PROCESSAMENTO SETOR: " + setor.id);
				writeFile(geoRelation, setor.id, estabelecimento.cnes, dirOutput, regiao);
				database.save(geoRelation);

			}

		}

	}

	public static void calcularRotasRelacionamento(String regiao, String dirOutput)
			throws ApiException, InterruptedException, IOException {

		GeoApiContext geoContext = new GeoApiContext.Builder().apiKey(API_KEY).build();

		CloudantClient client = ClientBuilder.url(new URL("http://142.93.196.115:5984")).username("admin")
				.password("mestrado123").build();

		Database database = client.database("mestrado-geo-relationship-" + regiao.toLowerCase(), true);
		boolean pula = true;

		for (RelacionamentoSetor relacionamento : relacionamentosEuclidianos) {

			if (pula) {

				if (relacionamento.objetoObservado.equalsIgnoreCase("SETOR.230765005000084")) {
					// pula = false;
					System.out.println("FIM DE PULO: " + relacionamento.id);
				} else {
					System.out.println("PULANDO: " + relacionamento.id);
					continue;
				}

			}

			Setor setor = getSetor(relacionamento.objetoObservado);
			Estabelecimento estabelecimento = getEstabelecimento(relacionamento.setorVizinho);

			if (setor == null || estabelecimento == null) {
				System.out.println("SETOR OU ESTABELECIMENTO NAO ENCONTRADO: " + relacionamento.id);
			}

			String origin = setor.centroMassLat + "," + setor.centroMassLng;
			String destination = estabelecimento.latitude + "," + estabelecimento.longitude;

			JSONObject geoRelation = new JSONObject();
			geoRelation.put("setor", setor.id);
			geoRelation.put("estabelecimento_cnes", estabelecimento.cnes);
			geoRelation.put("estabelecimento_tipo", estabelecimento.tipUnidade);
			geoRelation.put("estabelecimento_atencaoBasica", estabelecimento.atencaoBasica);
			geoRelation.put("estabelecimento_atendAmbulatorial", estabelecimento.atendAmbulatorial);
			geoRelation.put("estabelecimento_atendHospitalar", estabelecimento.atendHospitalar);
			geoRelation.put("estabelecimento_altaComplexidade", estabelecimento.altaComplexidade);
			geoRelation.put("estabelecimento_mediaComplexidade", estabelecimento.mediaComplexidade);
			geoRelation.put("estabelecimento_atividade", estabelecimento.atividade);
			geoRelation.put("estabelecimento_centroCirurgico", estabelecimento.centroCirurgico);
			geoRelation.put("estabelecimento_centroNeoNatal", estabelecimento.centroNeoNatal);
			geoRelation.put("estabelecimento_centroObstetrico", estabelecimento.centroObstetrico);
			geoRelation.put("estabelecimento_clientel", estabelecimento.clientel);
			geoRelation.put("estabelecimento_totalEquipes", estabelecimento.totalEquipes);
			geoRelation.put("estabelecimento_turnoAt", estabelecimento.turnoAt);
			geoRelation.put("estabelecimento_urgEmergencia", estabelecimento.urgEmergencia);
			geoRelation.put("estabelecimento_vincSUS", estabelecimento.vincSus);

			System.out.println("Processando SETOR: " + setor.id);

			DirectionsResult result = DirectionsApi.newRequest(geoContext).mode(TravelMode.DRIVING)
					// .avoid(
					// DirectionsApi.RouteRestriction.HIGHWAYS,
					// DirectionsApi.RouteRestriction.TOLLS,
					// DirectionsApi.RouteRestriction.FERRIES)
					.units(Unit.METRIC).origin(origin).destination(destination).await();

			geoRelation.put("rota_driving", result);

			result = DirectionsApi.newRequest(geoContext).mode(TravelMode.BICYCLING)
					// .avoid(
					// DirectionsApi.RouteRestriction.HIGHWAYS,
					// DirectionsApi.RouteRestriction.TOLLS,
					// DirectionsApi.RouteRestriction.FERRIES)
					.units(Unit.METRIC).origin(origin).destination(destination).await();

			geoRelation.put("rota_bicycling", result);

			result = DirectionsApi.newRequest(geoContext).mode(TravelMode.TRANSIT)
					// .avoid(
					// DirectionsApi.RouteRestriction.HIGHWAYS,
					// DirectionsApi.RouteRestriction.TOLLS,
					// DirectionsApi.RouteRestriction.FERRIES)
					.units(Unit.METRIC).origin(origin).destination(destination).await();

			geoRelation.put("rota_transit", result);

			result = DirectionsApi.newRequest(geoContext).mode(TravelMode.WALKING)
					// .avoid(
					// DirectionsApi.RouteRestriction.HIGHWAYS,
					// DirectionsApi.RouteRestriction.TOLLS,
					// DirectionsApi.RouteRestriction.FERRIES)
					.units(Unit.METRIC).origin(origin).destination(destination).await();

			geoRelation.put("rota_walking", result);

			System.out.println("FIM PROCESSAMENTO SETOR: " + setor.id);

			writeFile(geoRelation, setor.id, estabelecimento.cnes, dirOutput, regiao);
			database.save(geoRelation);
		}

	}

	public static void writeFile(JSONObject json, String setor, String cnes, String dirOutput, String regiao)
			throws IOException {
		BufferedWriter tWriterVizinhos = new BufferedWriter(
				new FileWriter(dirOutput + "\\" + regiao + "-" + setor + "-" + cnes + ".json"));
		Gson gson = new Gson();
		gson.toJson(json, tWriterVizinhos);
		tWriterVizinhos.flush();
		tWriterVizinhos.close();
	}

	public static void main(String[] args) throws IOException, ApiException, InterruptedException {

		String input_regiao = "CE";
		String dirOrigem = "C:\\projetos\\mestrado\\dados\\geojson-setores\\amostra";
		String outPut = "C:\\projetos\\mestrado\\processamento\\relacionamentos-geo-google-" + input_regiao + ".csv";
		String dirOutPut = "C:\\projetos\\mestrado\\processamento\\google";

		
		gerarArquivoSumarioRotas("C:\\projetos\\mestrado\\processamento\\google_setor_estabelecimento_1500mts", "CE", "C:\\projetos\\mestrado\\processamento\\relacionamentos");
		
		/**
		carregarSetores(dirOrigem, input_regiao);
		carregarEstabelecimentos(
				"C:\\projetos\\mestrado\\r-projeto\\mestrado-r\\to-gephi\\estabelecimentos-gephi-vertices-"
						+ input_regiao + ".csv");
		carregarRelacionamentos(
				"C:\\projetos\\mestrado\\r-projeto\\mestrado-r\\to-gephi\\setor-to-estabelecimento-gephi-"
						+ input_regiao + "-relacionamentos.csv");
		calcularRotasRelacionamento(input_regiao, dirOutPut);
		**/
	}

	public static Setor getSetor(String id) {
		id = id.substring(id.indexOf(".", 0) + 1, id.length());
		return setoresCache.get(id);

	}

	public static Estabelecimento getEstabelecimento(String id) {
		id = id.substring(id.indexOf(".", 0) + 1, id.length());
		for (Estabelecimento estabelecimento : estabelecimentos) {
			if (estabelecimento.id.equalsIgnoreCase(id)) {
				return estabelecimento;
			}
		}
		return null;
	}

	public static void carregarRelacionamentos(String dirObjetos) throws IOException {
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
			RelacionamentoSetor relacionamento = new RelacionamentoSetor();
			relacionamento.id = lineSplit[0];
			relacionamento.objetoObservado = lineSplit[1];
			relacionamento.setorVizinho = lineSplit[2];

			relacionamentosEuclidianos.add(relacionamento);
		}
	}

	public static void gerarArquivoSumarioRotas(String dirOrigem, String regiao, String outPut) throws IOException {
		File dir = new File(dirOrigem);
		BufferedWriter tWriter = new BufferedWriter(
				new FileWriter(outPut + "-" + regiao + "-SUMARIO-ROTAS.csv"));
		String line = "id,setor,cnes,dist_walk,tempo_walk,dist_drive,tempo_drive,dist_bike,tempo_bike,dist_transpub,tempo_transpub";
		tWriter.write(line);
		tWriter.newLine();
		tWriter.flush();
		
		for (File file : dir.listFiles()) {

			if (file.getName().startsWith(regiao)) {
				System.out.println("Avaliando Arquivo: " + file.getName());

				FileInputStream fileInput = new FileInputStream(file);
				byte[] content = new byte[(int) file.length()];
				fileInput.read(content);

				JSONObject json = new JSONObject(new String(content)).getJSONObject("map");
				
				line = "SETOR."+json.getString("setor")+"-"+"ST."+json.getString("estabelecimento_cnes")+",";
				line = line + json.getString("setor")+",";
				line = line + json.getString("estabelecimento_cnes")+",";
				
				if (json.getJSONObject("rota_walking").getJSONArray("routes").length() > 0) {
					line = line + json.getJSONObject("rota_walking").getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("distance").getInt("inMeters") +",";
					line = line + json.getJSONObject("rota_walking").getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("duration").getInt("inSeconds") +",";
				}
				else {
					line = line+"NA,NA,";
				}		
				if (json.getJSONObject("rota_walking").getJSONArray("routes").length() > 1) {
					System.out.println("RELACIONAMENTO: "+"SETOR."+json.getString("setor")+"-"+"ST."+json.getString("cnes")+ " POSSUI MAIS DE UMA ROTA DO TIPO WALK");
				}	
				
				
				if (json.getJSONObject("rota_driving").getJSONArray("routes").length() > 0) {
					line = line + json.getJSONObject("rota_driving").getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("distance").getInt("inMeters") +",";
					line = line + json.getJSONObject("rota_driving").getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("duration").getInt("inSeconds") +",";
				}
				else {
					line = line+"NA,NA,";
				}		
				if (json.getJSONObject("rota_driving").getJSONArray("routes").length() > 1) {
					System.out.println("RELACIONAMENTO: "+"SETOR."+json.getString("setor")+"-"+"ST."+json.getString("cnes")+ " POSSUI MAIS DE UMA ROTA DO TIPO WALK");
				}	
				
				
				if (json.getJSONObject("rota_bicycling").getJSONArray("routes").length() > 0) {
					line = line + json.getJSONObject("rota_bicycling").getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("distance").getInt("inMeters") +",";
					line = line + json.getJSONObject("rota_bicycling").getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("duration").getInt("inSeconds") +",";
				}
				else {
					line = line+"NA,NA,";
				}		
				if (json.getJSONObject("rota_bicycling").getJSONArray("routes").length() > 1) {
					System.out.println("RELACIONAMENTO: "+"SETOR."+json.getString("setor")+"-"+"ST."+json.getString("cnes")+ " POSSUI MAIS DE UMA ROTA DO TIPO WALK");
				}	
				
				
				if (json.getJSONObject("rota_transit").getJSONArray("routes").length() > 0) {
					line = line + json.getJSONObject("rota_transit").getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("distance").getInt("inMeters") +",";
					line = line + json.getJSONObject("rota_transit").getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("duration").getInt("inSeconds");
				}
				else {
					line = line+"NA,NA";
				}		
				if (json.getJSONObject("rota_transit").getJSONArray("routes").length() > 1) {
					System.out.println("RELACIONAMENTO: "+"SETOR."+json.getString("setor")+"-"+"ST."+json.getString("cnes")+ " POSSUI MAIS DE UMA ROTA DO TIPO WALK");
				}	
				
				tWriter.write(line);
				tWriter.newLine();
				tWriter.flush();
					
				
			} else {
				System.out.println("Ignorando Arquivo: " + file.getName());
			}
			
			

		}
		tWriter.close();
	}

}
