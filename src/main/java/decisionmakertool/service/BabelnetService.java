package decisionmakertool.service;

import com.fasterxml.jackson.databind.JsonNode;
import decisionmakertool.entities.Babelnet;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BabelnetService {

    private String url = "https://babelnet.io/v6/getSynsetIds?lemma=";
    private final RestTemplate restTemplate = new RestTemplate();

    public BabelnetService(){}

    public boolean existWord(String word) {
        String url = "https://babelnet.io/v6/getSynsetIds?lemma=";
        url+= word+"&searchLang=EN&key=0812565c-f78e-40eb-8020-69fae319234b";
        Babelnet[] response = restTemplate.getForObject(url, Babelnet[].class);
        System.out.println(response.length);
        if(response.length > 0){
            return true;
        }else{
            return false;
        }
    }

    public String getPOS(String word) {
        url+=word +"&searchLang=EN&key=0812565c-f78e-40eb-8020-69fae319234b";
        Babelnet[] response = restTemplate.getForEntity(url, Babelnet[].class).getBody();
        List<String> list = (Arrays.stream(response).map(x -> x.getPos())).collect(Collectors.toList());
        int cont = 0;
        String pos = "";
        for (String item : list) {
            int frequency = Collections.frequency(list, item);
            if(frequency > cont){
                cont = frequency;
                pos = item;
            }
        }
        return pos;
    }

    public ResponseEntity<JsonNode> getRelations(String id){
        String urlSynsets = "https://babelnet.io/v6/getOutgoingEdges?id=b" + id +"&key=0812565c-f78e-40eb-8020-69fae319234b"
        return restTemplate.getForEntity(url, JsonNode.class);
    }

    public List<String> getIds(String word) {
        url+=word +"&searchLang=EN&key=0812565c-f78e-40eb-8020-69fae319234b";
        Babelnet[] response = restTemplate.getForEntity(url, Babelnet[].class).getBody();
       return  (Arrays.stream(response).map(x -> x.getId())).collect(Collectors.toList());
    }
}
