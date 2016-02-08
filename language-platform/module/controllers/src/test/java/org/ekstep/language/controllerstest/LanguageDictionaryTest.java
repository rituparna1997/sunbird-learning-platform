package org.ekstep.language.controllerstest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.ekstep.language.mgr.impl.DictionaryManagerImpl;
import org.ekstep.language.router.LanguageRequestRouterPool;
import org.ekstep.language.test.util.RequestResponseTestHelper;
import org.ekstep.language.util.ElasticSearchUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ilimi.common.dto.Request;
import com.ilimi.common.dto.Response;
import com.ilimi.taxonomy.mgr.impl.TaxonomyManagerImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration({ "classpath:servlet-context.xml" })
public class LanguageDictionaryTest {

	@Autowired
	private WebApplicationContext context;
	private static DictionaryManagerImpl dictionaryManager = new DictionaryManagerImpl();
	private static TaxonomyManagerImpl taxonomyManager = new TaxonomyManagerImpl();
	private static ObjectMapper mapper = new ObjectMapper();
	private ResultActions actions;
	static ElasticSearchUtil util;
	private static String TEST_LANGUAGE = "testdictionary";
	private static String TEST_CREATE_LANGUAGE = "testcreatedictionary";

	static {
		LanguageRequestRouterPool.init();
	}

	@BeforeClass
	public static void init() throws Exception {
		// Definitions
		deleteDefinitionStatic(TEST_LANGUAGE);
		deleteDefinitionStatic(TEST_CREATE_LANGUAGE);
		createDefinitionsStatic(TEST_LANGUAGE);
		createDefinitionsStatic(TEST_CREATE_LANGUAGE);
		createWord();
	}

	@AfterClass
	public static void close() throws IOException, InterruptedException {
		deleteDefinitionStatic(TEST_LANGUAGE);
		deleteDefinitionStatic(TEST_CREATE_LANGUAGE);
	}

	@SuppressWarnings("rawtypes")
	private static void createWord() throws JsonParseException,
			JsonMappingException, IOException {
		String contentString = "{\"request\":{\"words\":[{\"identifier\":\"en_w_707\",\"lemma\":\"newtestword\",\"difficultyLevel\":\"Easy\",\"synonyms\":[{\"identifier\":\"202707688\",\"gloss\":\"newsynonym\"}],\"antonyms\":[{\"name\":\"newtestwordantonym\"}],\"tags\":[\"English\",\"API\"]}]}}";
		String expectedResult = "[\"en_w_707\"]";
		Map<String, Object> map = mapper.readValue(contentString,
				new TypeReference<Map<String, Object>>() {
				});
		Request request = RequestResponseTestHelper.getRequest(map);
		Response response = dictionaryManager.create(TEST_LANGUAGE, "Word",
				request);
		Assert.assertEquals("successful", response.getParams().getStatus());
		Map<String, Object> result = response.getResult();
		List nodeIds = (List) result.get("node_id");
		String resultString = mapper.writeValueAsString(nodeIds);
		Assert.assertEquals(expectedResult, resultString);

	}

	@SuppressWarnings("rawtypes")
	@Test
	public void createWordTest() throws JsonParseException,
			JsonMappingException, IOException {
		String contentString = "{\"request\":{\"words\":[{\"identifier\":\"en_w_708\",\"lemma\":\"newtestword\",\"difficultyLevel\":\"Easy\",\"synonyms\":[{\"identifier\":\"202707688\",\"gloss\":\"newsynonym\"}],\"antonyms\":[{\"name\":\"newtestwordantonym\"}],\"tags\":[\"English\",\"API\"]}]}}";
		String expectedResult = "[\"en_w_708\"]";
		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v1/language/dictionary/word/" + TEST_CREATE_LANGUAGE;
		try {
			actions = mockMvc.perform(MockMvcRequestBuilders.post(path)
					.contentType(MediaType.APPLICATION_JSON)
					.content(contentString.getBytes())
					.header("user-id", "ilimi"));
			Assert.assertEquals(200, actions.andReturn().getResponse()
					.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
		}

		Response response = jsonToObject(actions);
		Assert.assertEquals("successful", response.getParams().getStatus());
		Map<String, Object> result = response.getResult();
		List nodeIds = (List) result.get("node_id");
		String resultString = mapper.writeValueAsString(nodeIds);
		Assert.assertEquals(expectedResult, resultString);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void searchWord() throws JsonParseException, JsonMappingException,
			IOException {
		String contentString = "{\"request\":{\"lemma\":[\"newtestword\"],\"limit\":10}}";
		String lemma = "newtestword";
		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v1/language/dictionary/search/" + TEST_LANGUAGE;
		try {
			actions = mockMvc.perform(MockMvcRequestBuilders.post(path)
					.contentType(MediaType.APPLICATION_JSON)
					.content(contentString.getBytes())
					.header("user-id", "ilimi"));
			Assert.assertEquals(200, actions.andReturn().getResponse()
					.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
		}
		Response resp = jsonToObject(actions);
		Assert.assertEquals("successful", resp.getParams().getStatus());
		Map<String, Object> result = resp.getResult();
		ArrayList<Map<String, Object>> words = (ArrayList<Map<String, Object>>) result
				.get("words");
		for(Map<String, Object> wordMap : words){
			Assert.assertEquals(wordMap.get("lemma"), lemma);
			Assert.assertEquals(wordMap.get("language"), TEST_LANGUAGE);
		}
	}
	
	@Test
	public void updateWordTest() throws JsonParseException,
			JsonMappingException, IOException {
		String contentString = "{\"request\":{\"word\":{\"lemma\":\"newtestword\",\"synonyms\":[{\"identifier\":\"202707688\",\"gloss\":\"newsynonym\",\"words\":[\"newtestword\"]}],\"antonyms\":[{\"name\":\"newtestwordantonym\"}]}}}";
		String expectedResult = "\"en_w_707\"";
		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v1/language/dictionary/word/" + TEST_CREATE_LANGUAGE
				+ "/en_w_707";
		try {
			actions = mockMvc.perform(MockMvcRequestBuilders.patch(path)
					.contentType(MediaType.APPLICATION_JSON)
					.content(contentString.getBytes())
					.header("user-id", "ilimi"));
			Assert.assertEquals(200, actions.andReturn().getResponse()
					.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
		}

		Response response = jsonToObject(actions);
		Assert.assertEquals("successful", response.getParams().getStatus());
		Map<String, Object> result = response.getResult();
		String nodeIds = (String) result.get("node_id");
		String resultString = mapper.writeValueAsString(nodeIds);
		Assert.assertEquals(expectedResult, resultString);
	}

	@Test
	public void addRelation() throws JsonParseException, JsonMappingException,
			IOException {
		String expectedResult = "\"" + TEST_LANGUAGE + "\"";
		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v1/language/dictionary/word/" + TEST_LANGUAGE
				+ "/202707688/synonym/en_w_707";
		try {
			actions = mockMvc.perform(MockMvcRequestBuilders.post(path)
					.contentType(MediaType.APPLICATION_JSON)
					.header("user-id", "ilimi"));
			Assert.assertEquals(200, actions.andReturn().getResponse()
					.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
		}

		Response response = jsonToObject(actions);
		Assert.assertEquals("successful", response.getParams().getStatus());
		Map<String, Object> result = response.getResult();
		String nodeIds = (String) result.get("graph_id");
		String resultString = mapper.writeValueAsString(nodeIds);
		Assert.assertEquals(expectedResult, resultString);
	}

	@Test
	public void upload() throws JsonParseException, JsonMappingException,
			IOException {
		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v1/language/dictionary/word/media/upload";
		MockMultipartFile testFile = new MockMultipartFile("file",
				"testSsf.txt", "text/plain", "file".getBytes());
		try {
			actions = mockMvc.perform(MockMvcRequestBuilders.fileUpload(path)
					.file(testFile).header("user-id", "ilimi"));
			/*Assert.assertEquals(200, actions.andReturn().getResponse()
					.getStatus());*/
		} catch (Exception e) {
			e.printStackTrace();
		}

		Response response = jsonToObject(actions);
		Assert.assertEquals("successful", response.getParams().getStatus());
	}

	@Test
	public void deleteRelation() throws JsonParseException,
			JsonMappingException, IOException {
		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v1/language/dictionary/word/" + TEST_CREATE_LANGUAGE
				+ "/202707688/synonym/en_w_707";
		try {
			actions = mockMvc.perform(MockMvcRequestBuilders.delete(path)
					.contentType(MediaType.APPLICATION_JSON)
					.header("user-id", "ilimi"));
			Assert.assertEquals(200, actions.andReturn().getResponse()
					.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
		}

		Response response = jsonToObject(actions);
		Assert.assertEquals("successful", response.getParams().getStatus());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getWord() throws JsonParseException, JsonMappingException,
			IOException {
		/*String expectedResult = "{\"identifier\":\"en_w_707\",\"difficultyLevel\":\"Easy\",\"synonyms\":[{\"key1\":\"value1\",\"key2\":\"value2\",\"identifier\":\"202707688\",\"status\":\"Draft\"},{\"identifier\":\"201339774\",\"status\":\"Draft\"},{\"identifier\":\"108242255\",\"status\":\"Draft\"},{\"identifier\":\""
				+ TEST_LANGUAGE
				+ "_5\",\"gloss\":\"newsynonym\",\"status\":\"Draft\"}],\"antonyms\":[{\"identifier\":\""
				+ TEST_LANGUAGE
				+ "_6\",\"name\":\"newtestword3\",\"objectType\":\"Word\",\"relation\":\"hasAntonym\",\"index\":null},{\"identifier\":\""
				+ TEST_LANGUAGE
				+ "_8\",\"name\":\"newtestword431\",\"objectType\":\"Word\",\"relation\":\"hasAntonym\",\"index\":null}],\"lastUpdatedOn\":\"test\",\"lemma\":\"test_create_word_707\",\"language\":\""
				+ TEST_LANGUAGE
				+ "\",\"createdOn\":\"test\",\"status\":\"Live\",\"tags\":[\"English\",\"API\"]}";*/
		String identifier = "en_w_707";
		String lemma = "newtestword";
		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v1/language/dictionary/word/" + TEST_LANGUAGE
				+ "/en_w_707";
		try {
			actions = mockMvc.perform(MockMvcRequestBuilders.get(path).header(
					"user-id", "ilimi"));
			Assert.assertEquals(200, actions.andReturn().getResponse()
					.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
		}
		Response response = jsonToObject(actions);
		Assert.assertEquals("successful", response.getParams().getStatus());
		Map<String, Object> result = response.getResult();
		Map<String, Object> wordMap = (Map<String, Object>) result.get("Word");
		Assert.assertEquals(wordMap.get("identifier"), identifier);
		Assert.assertEquals(wordMap.get("lemma"), lemma);
		Assert.assertEquals(wordMap.get("language"), TEST_LANGUAGE);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getSynonyms() throws JsonParseException, JsonMappingException,
			IOException {
		String lemma = "newtestword";
		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v1/language/dictionary/word/" + TEST_LANGUAGE
				+ "/synonym/202707688";
		try {
			actions = mockMvc.perform(MockMvcRequestBuilders.get(path).header(
					"user-id", "ilimi"));
			Assert.assertEquals(200, actions.andReturn().getResponse()
					.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
		}
		Response response = jsonToObject(actions);
		Assert.assertEquals("successful", response.getParams().getStatus());
		Map<String, Object> result = response.getResult();
		ArrayList<Map<String, Object>> words = (ArrayList<Map<String, Object>>) result
				.get("synonyms");
		for(Map<String, Object> wordMap : words){
			Assert.assertEquals(wordMap.get("lemma"), lemma);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getWords() throws JsonParseException, JsonMappingException,
			IOException {
/*		String expectedResult = "{\"words\":[{\"identifier\":\""
				+ TEST_LANGUAGE
				+ "_6\",\"antonyms\":[{\"identifier\":\"en_w_707\",\"name\":\"test_create_word_707\",\"objectType\":\"Word\",\"relation\":\"hasAntonym\",\"index\":null}],\"lastUpdatedOn\":\"test\",\"lemma\":\"newtestword3\",\"language\":\""
				+ TEST_LANGUAGE
				+ "\",\"createdOn\":\"test\",\"status\":\"Live\"},{\"lastUpdatedOn\":\"test\",\"lemma\":\"newtestword4\",\"identifier\":\""
				+ TEST_LANGUAGE
				+ "_7\",\"language\":\""
				+ TEST_LANGUAGE
				+ "\",\"createdOn\":\"test\",\"status\":\"Live\"},{\"identifier\":\""
				+ TEST_LANGUAGE
				+ "_8\",\"antonyms\":[{\"identifier\":\"en_w_707\",\"name\":\"test_create_word_707\",\"objectType\":\"Word\",\"relation\":\"hasAntonym\",\"index\":null}],\"lastUpdatedOn\":\"test\",\"lemma\":\"newtestword431\",\"language\":\""
				+ TEST_LANGUAGE
				+ "\",\"createdOn\":\"test\",\"status\":\"Live\"},{\"lastUpdatedOn\":\"test\",\"lemma\":\"newtestword433\",\"identifier\":\""
				+ TEST_LANGUAGE
				+ "_9\",\"language\":\""
				+ TEST_LANGUAGE
				+ "\",\"createdOn\":\"test\",\"status\":\"Live\"},{\"identifier\":\"en_w_707\",\"difficultyLevel\":\"Easy\",\"synonyms\":[{\"key1\":\"value1\",\"key2\":\"value2\",\"identifier\":\"202707688\",\"status\":\"Draft\"},{\"identifier\":\"201339774\",\"status\":\"Draft\"},{\"identifier\":\"108242255\",\"status\":\"Draft\"},{\"identifier\":\""
				+ TEST_LANGUAGE
				+ "_5\",\"gloss\":\"newsynonym\",\"status\":\"Draft\"}],\"antonyms\":[{\"identifier\":\""
				+ TEST_LANGUAGE
				+ "_6\",\"name\":\"newtestword3\",\"objectType\":\"Word\",\"relation\":\"hasAntonym\",\"index\":null},{\"identifier\":\""
				+ TEST_LANGUAGE
				+ "_8\",\"name\":\"newtestword431\",\"objectType\":\"Word\",\"relation\":\"hasAntonym\",\"index\":null}],\"lastUpdatedOn\":\"test\",\"lemma\":\"test_create_word_707\",\"language\":\""
				+ TEST_LANGUAGE
				+ "\",\"createdOn\":\"test\",\"status\":\"Live\",\"tags\":[\"English\",\"API\"]}]}";*/

		String lemma = "newtestword";
		String lemmaAntonym = "newtestwordantonym";

		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v1/language/dictionary/word/" + TEST_LANGUAGE;
		try {
			actions = mockMvc.perform(MockMvcRequestBuilders.get(path).header(
					"user-id", "ilimi"));
			Assert.assertEquals(200, actions.andReturn().getResponse()
					.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
		}
		Response response = jsonToObject(actions);
		Assert.assertEquals("successful", response.getParams().getStatus());
		Map<String, Object> result = response.getResult();
		ArrayList<Map<String, Object>> words = (ArrayList<Map<String, Object>>) result
				.get("words");
		for(Map<String, Object> wordMap : words){
			if(!((String)wordMap.get("lemma")).contains("antonym")){
				Assert.assertEquals(wordMap.get("lemma"), lemma);
			}
			else{
				Assert.assertEquals(wordMap.get("lemma"), lemmaAntonym);
			}
			Assert.assertEquals(wordMap.get("language"), TEST_LANGUAGE);
		}
	}

	public static void createDefinitionsStatic(String language) {
		String contentString = "{  \"definitionNodes\": [    {      \"objectType\": \"Word\",      \"properties\": [        {          \"propertyName\": \"lemma\",          \"title\": \"Lemma\",          \"description\": \"\",          \"category\": \"general\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": true,          \"indexed\": true,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 3 }\"        },        {          \"propertyName\": \"sources\",          \"title\": \"Sources\",          \"description\": \"\",          \"category\": \"general\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": true,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'text', 'order': 2 }\"        },        {          \"propertyName\": \"sourceTypes\",          \"title\": \"Source Types\",          \"description\": \"\",          \"category\": \"general\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": true,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'text', 'order': 2 }\"        },        {          \"propertyName\": \"commisionedBy\",          \"title\": \"Commisioned By\",          \"description\": \"\",          \"category\": \"general\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": true,          \"displayProperty\": \"Readonly\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'text', 'order': 5 }\"        },        {          \"propertyName\": \"defaultSynset\",          \"title\": \"Default Synset\",          \"description\": \"\",          \"category\": \"general\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": true,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'text', 'order': 1 }\"        },        {          \"propertyName\": \"syllableCount\",          \"title\": \"Syllable Count\",          \"description\": \"\",          \"category\": \"lexile\",          \"dataType\": \"Number\",          \"range\": [],          \"required\": false,          \"indexed\": true,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'text', 'order': 4 }\"        },        {          \"propertyName\": \"syllableNotation\",          \"title\": \"Syllable Notation\",          \"description\": \"\",          \"category\": \"lexile\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Readonly\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'textarea', 'order': 4 }\"        },        {          \"propertyName\": \"unicodeNotation\",          \"title\": \"Unicode Notation\",          \"description\": \"\",          \"category\": \"lexile\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Readonly\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'textarea', 'order': 4 }\"        },        {          \"propertyName\": \"rtsNotation\",          \"title\": \"RTS Notation\",          \"description\": \"\",          \"category\": \"lexile\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'textarea', 'order': 4 }\"        },        {          \"propertyName\": \"vectorsRepresentation\",          \"title\": \"Vectors representation\",          \"description\": \"\",          \"category\": \"lexile\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": true,          \"displayProperty\": \"Readonly\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'select',  'order': 9 }\"        },        {          \"propertyName\": \"orthographicFeatures\",          \"title\": \"Orthographic Features\",          \"description\": \"\",          \"category\": \"lexile\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"\"        },        {          \"propertyName\": \"orthographic_complexity\",          \"title\": \"Orthographic Complexity\",          \"description\": \"\",          \"category\": \"lexile\",          \"dataType\": \"Number\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'textarea',  'order': 4 }\"        },        {          \"propertyName\": \"phonologic_complexity\",          \"title\": \"Phonological Complexity\",          \"description\": \"\",          \"category\": \"lexile\",          \"dataType\": \"Number\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'textarea',  'order': 4 }\"        },        {          \"propertyName\": \"pos\",          \"title\": \"POS (Parts of Speech)\",          \"description\": \"\",          \"category\": \"grammar\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'textarea', 'order': 19 }\"        },        {          \"propertyName\": \"grade\",          \"title\": \"Grade\",          \"description\": \"\",          \"category\": \"grammar\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'textarea', 'order': 19 }\"        },        {          \"propertyName\": \"morphology\",          \"title\": \"Morphology\",          \"description\": \"\",          \"category\": \"grammar\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 4 }\"        },        {          \"propertyName\": \"parts\",          \"title\": \"Parts\",          \"description\": \"\",          \"category\": \"grammar\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 4 }\"        },        {          \"propertyName\": \"affixes\",          \"title\": \"Affixes\",          \"description\": \"\",          \"category\": \"grammar\",          \"dataType\": \"List\",          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'textarea', 'order': 14 }\"        },        {          \"propertyName\": \"namedEntityType\",          \"title\": \"Named Entity Type\",          \"description\": \"\",          \"category\": \"grammar\",          \"dataType\": \"List\",          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'select', 'order': 14 }\"        },        {          \"propertyName\": \"loanWordSourceLanguage\",          \"title\": \"Loan Word Source Language\",          \"description\": \"\",          \"category\": \"grammar\",          \"dataType\": \"Select\",          \"range\": [            \"hi\",            \"en\",            \"ka\",            \"te\"          ],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'select',  'order': 14 }\"        },        {          \"propertyName\": \"ageBand\",          \"title\": \"Age Band\",          \"description\": \"\",          \"category\": \"pedagogy\",          \"dataType\": \"Select\",          \"range\": [            \"1-5\",            \"6-10\",            \"11-15\",            \"16-20\"          ],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'select', 'order': 14 }\"        },        {          \"propertyName\": \"microConcepts\",          \"title\": \"Micro Concepts\",          \"description\": \"\",          \"category\": \"pedagogy\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'teaxtarea', 'order': 14 }\"        },        {          \"propertyName\": \"difficultyLevel\",          \"title\": \"Difficulty Level\",          \"description\": \"\",          \"category\": \"pedagogy\",          \"dataType\": \"Select\",          \"range\": [            \"Easy\",            \"Medium\",            \"Difficult\"          ],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'select', 'order': 4 }\"        },        {          \"propertyName\": \"occurrenceCount\",          \"title\": \"Occurrence Count\",          \"description\": \"\",          \"category\": \"frequency\",          \"dataType\": \"Number\",          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'textarea', 'order': 4 }\"        },        {          \"propertyName\": \"senseSetCount\",          \"title\": \"Sense Set Count\",          \"description\": \"\",          \"category\": \"frequency\",          \"dataType\": \"Number\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'Number', 'order': 4 }\"        },        {          \"propertyName\": \"posCount\",          \"title\": \"Parts of the Speech Count\",          \"description\": \"\",          \"category\": \"frequency\",          \"dataType\": \"Number\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'number', 'order': 4 }\"        },        {          \"propertyName\": \"userSets\",          \"title\": \"User Sets\",          \"description\": \"\",          \"category\": \"frequency\",          \"dataType\": \"Number\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'number', 'order': 4 }\"        },        {          \"propertyName\": \"sampleUsages\",          \"title\": \"Sample Usages\",          \"description\": \"\",          \"category\": \"sampleData\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"\"        },        {          \"propertyName\": \"audio\",          \"title\": \"Audio\",          \"description\": \"\",          \"category\": \"sampleData\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': '',  'order': 9 }\"        },        {          \"propertyName\": \"pictures\",          \"title\": \"Pictures\",          \"description\": \"\",          \"category\": \"sampleData\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": true,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': '',  'order': 14 }\"        },        {          \"propertyName\": \"pronunciations\",          \"title\": \"Pronunciations\",          \"description\": \"\",          \"category\": \"sampleData\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": true,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': '',  'order': 14 }\"        },        {          \"propertyName\": \"reviewers\",          \"title\": \"Reviewers\",          \"description\": \"\",          \"category\": \"audit\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'text',  'order': 14 }\"        },        {          \"propertyName\": \"lastUpdatedBy\",          \"title\": \"Last Updated By\",          \"description\": \"\",          \"category\": \"audit\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'text',  'order': 14 }\"        },        {          \"propertyName\": \"lastUpdatedOn\",          \"title\": \"Last Updated On\",          \"description\": \"\",          \"category\": \"audit\",          \"dataType\": \"Date\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Readonly\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 22 }\"        },        {            \"propertyName\": \"status\",            \"title\": \"Status\",            \"description\": \"Status of the domain\",            \"category\": \"audit\",            \"dataType\": \"Select\",            \"range\":            [                \"Draft\",                \"Live\",                \"Review\",                \"Retired\"            ],            \"required\": false,			\"indexed\": true,            \"displayProperty\": \"Editable\",            \"defaultValue\": \"Live\",            \"renderingHints\": \"{'inputType': 'select', 'order': 23}\"        },        {          \"propertyName\": \"source\",          \"title\": \"Source\",          \"description\": \"\",          \"category\": \"conflicts\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Readonly\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 25 }\"        },        {          \"propertyName\": \"conflictStatus\",          \"title\": \"Conflict Status\",          \"description\": \"\",          \"category\": \"conflicts\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Readonly\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 25 }\"        },        {          \"propertyName\": \"relevancy\",          \"title\": \"Relevancy\",          \"description\": \"\",          \"category\": \"analytics\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Readonly\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 25 }\"        },        {          \"propertyName\": \"complexity\",          \"title\": \"Complexity\",          \"description\": \"\",          \"category\": \"analytics\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Readonly\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 25 }\"        },        {          \"propertyName\": \"possibleSpellings\",          \"title\": \"Possible Spellings\",          \"description\": \"\",          \"category\": \"supportability\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 25 }\"        },        {          \"propertyName\": \"allowedSuffixes\",          \"title\": \"Allowed Suffixes\",          \"description\": \"\",          \"category\": \"supportability\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 25 }\"        },        {          \"propertyName\": \"allowedPrefixes\",          \"title\": \"Allowed Prefixes\",          \"description\": \"\",          \"category\": \"supportability\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 25 }\"        },        {          \"propertyName\": \"allowedInfixes\",          \"title\": \"Allowed Infixes\",          \"description\": \"\",          \"category\": \"supportability\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 25 }\"        },        {          \"propertyName\": \"tenseForms\",          \"title\": \"Tense Forms\",          \"description\": \"\",          \"category\": \"supportability\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 25 }\"        },        {          \"propertyName\": \"pluralForms\",          \"title\": \"Plural Forms\",          \"description\": \"\",          \"category\": \"supportability\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 25 }\"        },        {          \"propertyName\": \"singularForms\",          \"title\": \"Singular Forms\",          \"description\": \"\",          \"category\": \"supportability\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 25 }\"        },        {          \"propertyName\": \"genders\",          \"title\": \"Genders\",          \"description\": \"\",          \"category\": \"supportability\",          \"dataType\": \"List\",          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'select', 'order': 25 }\"        },        {          \"propertyName\": \"pronouns\",          \"title\": \"Pronouns\",          \"description\": \"\",          \"category\": \"supportability\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 25 }\"        }      ],      \"inRelations\": [        {          \"relationName\": \"synonym\",          \"title\": \"synonyms\",          \"description\": \"\",          \"required\": false,          \"objectTypes\": [            \"Synset\"          ],          \"renderingHints\": \"{ 'order': 26 }\"        }      ],      \"outRelations\": [        {          \"relationName\": \"hasAntonym\",          \"title\": \"antonyms\",          \"description\": \"\",          \"required\": false,          \"objectTypes\": [            \"Word\"          ],          \"renderingHints\": \"{ 'order': 26 }\"        },        {          \"relationName\": \"hasHypernym\",          \"title\": \"hypernyms\",          \"description\": \"\",          \"required\": false,          \"objectTypes\": [            \"Word\"          ],          \"renderingHints\": \"{ 'order': 26 }\"        },        {          \"relationName\": \"hasHolonym\",          \"title\": \"holonyms\",          \"description\": \"\",          \"required\": false,          \"objectTypes\": [            \"Word\"          ],          \"renderingHints\": \"{ 'order': 26 }\"        },        {          \"relationName\": \"hasHyponym\",          \"title\": \"hyponyms\",          \"description\": \"\",          \"required\": false,          \"objectTypes\": [            \"Word\"          ],          \"renderingHints\": \"{ 'order': 26 }\"        },        {          \"relationName\": \"hasMeronym\",          \"title\": \"meronyms\",          \"description\": \"\",          \"required\": false,          \"objectTypes\": [            \"Word\"          ],          \"renderingHints\": \"{ 'order': 26 }\"        }      ],      \"systemTags\": [        {          \"name\": \"Review Tags\",          \"description\": \"Need to Review this Word.\"        },        {          \"name\": \"Missing Information\",          \"description\": \"Some the information is missing.\"        },        {          \"name\": \"Incorrect Data\",          \"description\": \"Wrong information about this word.\"        },        {          \"name\": \"Spelling Mistakes\",          \"description\": \"Incorrect Spellings\"        }      ],      \"metadata\": {        \"ttl\": 24,        \"limit\": 50      }    }  ]}";
		taxonomyManager.updateDefinition(language, contentString);
		contentString = "{  \"definitionNodes\": [    {      \"objectType\": \"Synset\",      \"properties\": [        {          \"propertyName\": \"gloss\",          \"title\": \"Gloss\",          \"description\": \"\",          \"category\": \"general\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": true,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 3 }\"        },        {          \"propertyName\": \"glossInEnglish\",          \"title\": \"Gloss in English\",          \"description\": \"\",          \"category\": \"general\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": true,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'text', 'order': 5 }\"        },        {          \"propertyName\": \"exampleSentences\",          \"title\": \"Example Sentences\",          \"description\": \"\",          \"category\": \"general\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": true,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'text', 'order': 1 }\"        },        {          \"propertyName\": \"frames\",          \"title\": \"Sentence Frames\",          \"description\": \"\",          \"category\": \"general\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": true,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'text', 'order': 1 }\"        },        {          \"propertyName\": \"pos\",          \"title\": \"POS\",          \"description\": \"\",          \"category\": \"grammar\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'textarea', 'order': 19 }\"        },        {          \"propertyName\": \"namedEntityType\",          \"title\": \"Named Entity Type\",          \"description\": \"\",          \"category\": \"grammar\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 4 }\"        },        {          \"propertyName\": \"pictures\",          \"title\": \"Pictures\",          \"description\": \"URL\",          \"category\": \"sampleData\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"\"        },        {          \"propertyName\": \"Audio\",          \"title\": \"audio\",          \"description\": \"URL\",          \"category\": \"sampleData\",          \"dataType\": \"List\",          \"range\": [],          \"required\": false,          \"indexed\": true,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'teaxtarea',  'order': 9 }\"        },        {          \"propertyName\": \"reviewers\",          \"title\": \"Reviewers\",          \"description\": \"\",          \"category\": \"audit\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'text',  'order': 14 }\"        },        {          \"propertyName\": \"lastUpdatedBy\",          \"title\": \"Last Updated By\",          \"description\": \"\",          \"category\": \"audit\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Editable\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'inputType': 'text',  'order': 14 }\"        },        {          \"propertyName\": \"lastUpdatedOn\",          \"title\": \"Last Updated On\",          \"description\": \"\",          \"category\": \"audit\",          \"dataType\": \"Date\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Readonly\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 22 }\"        },        {            \"propertyName\": \"status\",            \"title\": \"Status\",            \"description\": \"Status of the domain\",            \"category\": \"audit\",            \"dataType\": \"Select\",            \"range\":            [                \"Draft\",                \"Live\",                \"Retired\"            ],            \"required\": false,			\"indexed\": true,            \"displayProperty\": \"Editable\",            \"defaultValue\": \"Draft\",            \"renderingHints\": \"{'inputType': 'select', 'order': 23}\"        },        {          \"propertyName\": \"source\",          \"title\": \"Source\",          \"description\": \"\",          \"category\": \"conflicts\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Readonly\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 25 }\"        },        {          \"propertyName\": \"conflictStatus\",          \"title\": \"Conflict Status\",          \"description\": \"\",          \"category\": \"conflicts\",          \"dataType\": \"Text\",          \"range\": [],          \"required\": false,          \"indexed\": false,          \"displayProperty\": \"Readonly\",          \"defaultValue\": \"\",          \"renderingHints\": \"{ 'order': 25 }\"        }      ],      \"inRelations\": [],      \"outRelations\": [        {          \"relationName\": \"synonym\",          \"title\": \"Synonyms\",          \"description\": \"\",          \"required\": false,          \"objectTypes\": [            \"Word\"          ],          \"renderingHints\": \"{ 'order': 26 }\"        },        {          \"relationName\": \"hasAntonym\",          \"title\": \"Antonyms\",          \"description\": \"\",          \"required\": false,          \"objectTypes\": [            \"Synset\"          ],          \"renderingHints\": \"{ 'order': 26 }\"        },        {          \"relationName\": \"hasHypernym\",          \"title\": \"Hypernyms\",          \"description\": \"\",          \"required\": false,          \"objectTypes\": [            \"Synset\"          ],          \"renderingHints\": \"{ 'order': 26 }\"        },        {          \"relationName\": \"hasHolonym\",          \"title\": \"Holonyms\",          \"description\": \"\",          \"required\": false,          \"objectTypes\": [            \"Synset\"          ],          \"renderingHints\": \"{ 'order': 26 }\"        },        {          \"relationName\": \"hasHyponym\",          \"title\": \"Hyponyms\",          \"description\": \"\",          \"required\": false,          \"objectTypes\": [            \"Synset\"          ],          \"renderingHints\": \"{ 'order': 26 }\"        },        {          \"relationName\": \"hasMeronym\",          \"title\": \"Meronyms\",          \"description\": \"\",          \"required\": false,          \"objectTypes\": [            \"Synset\"          ],          \"renderingHints\": \"{ 'order': 26 }\"        }      ],      \"systemTags\": [        {          \"name\": \"Review Tags\",          \"description\": \"Need to Review this Synset.\"        },        {          \"name\": \"Missing Information\",          \"description\": \"Some the information is missing.\"        },        {          \"name\": \"Incorrect Data\",          \"description\": \"Wrong information about this Synset.\"        }      ],      \"metadata\": {        \"ttl\": 24,        \"limit\": 50      }    }  ]}";
		taxonomyManager.updateDefinition(language, contentString);
	}

	public static void deleteDefinitionStatic(String language) {
		taxonomyManager.delete(language);
	}

	public static Response jsonToObject(ResultActions actions) {
		String content = null;
		Response resp = null;
		try {
			content = actions.andReturn().getResponse().getContentAsString();
			ObjectMapper objectMapper = new ObjectMapper();
			if (StringUtils.isNotBlank(content))
				resp = objectMapper.readValue(content, Response.class);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resp;
	}
}
