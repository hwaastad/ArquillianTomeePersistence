/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.waastad.arquillianpersistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.persistence.UsingDataSet;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.beans10.BeansDescriptor;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.waastad.arquillianpersistence.entity.UserAccount;
import org.waastad.arquillianpersistence.filter.TokenFilter;
import org.waastad.arquillianpersistence.producer.EntityManagerProducer;
import org.waastad.arquillianpersistence.repository.UserAccountRepository;
import org.waastad.arquillianpersistence.service.UserService;

/**
 *
 * @author Helge Waastad <helge.waastad@waastad.org>
 */
@RunWith(Arquillian.class)
public class RepositoryTest {

//    @ArquillianResource
//    private URL url;
    @Deployment
    public static Archive<?> createDeploymentPackage() {
        BeansDescriptor beans = Descriptors.create(BeansDescriptor.class).getOrCreateAlternatives().clazz("org.apache.deltaspike.jpa.impl.transaction.ContainerManagedTransactionStrategy").up();

        File[] libs = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve().withTransitivity().asFile();
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(UserService.class, TokenFilter.class, EntityManagerProducer.class, UserAccountRepository.class, UserAccount.class)
                .addAsWebInfResource(new StringAsset(beans.exportAsString()), "beans.xml")
                .addAsWebInfResource("test-persistence.xml", "persistence.xml")
                .addAsLibraries(libs);
    }

    @Inject
    private UserService userService;
    @Inject
    private UserAccountRepository userAccountRepository;


    @Test
    @UsingDataSet("users.yml")
    @Transactional(TransactionMode.COMMIT)
    @InSequence(value = 1)
    public void testSomeMethod2() {
        UserAccount findBy = userAccountRepository.findBy(1L);
        Assert.assertEquals("Frank", findBy.getFirstname());
    }

    @Test
    @UsingDataSet("users.yml")
    @Transactional(TransactionMode.COMMIT)
    @InSequence(value = 2)
    public void testSomeMethod3() throws Exception {
        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        WebClient client = WebClient.create(getUrl(), providers)
                .path("test/users")
                .header("token", "admin")
                .type(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
        ClientConfiguration config = WebClient.getConfig(client);
        config.getOutInterceptors().add(new LoggingOutInterceptor());
        config.getInInterceptors().add(new LoggingInInterceptor());
        List<UserAccount> get = (List<UserAccount>) client.getCollection(UserAccount.class);
        Assert.assertEquals(2, get.size());
    }

    @Test
    @UsingDataSet("users.yml")
    @InSequence(value = 3)
    public void testSomeMethod5() throws Exception {
        ObjectMapper mappe = new ObjectMapper();
        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        UserAccount findBy = new UserAccount("first", "last");
        WebClient client = WebClient.create(getUrl(), providers)
                .path("test/users")
                .type(MediaType.APPLICATION_JSON)
                .header("token", "admin")
                .accept(MediaType.APPLICATION_JSON);
        ClientConfiguration config = WebClient.getConfig(client);
        config.getOutInterceptors().add(new LoggingOutInterceptor());
        config.getInInterceptors().add(new LoggingInInterceptor());
        UserAccount post = client.post(findBy, UserAccount.class);
        System.out.println(mappe.writerWithDefaultPrettyPrinter().writeValueAsString(post));
        List<UserAccount> get = (List<UserAccount>) client.getCollection(UserAccount.class);
        Assert.assertEquals(3, get.size());
    }

    private String getUrl() throws MalformedURLException {
        URL url = new URL("http://localhost:" + System.getProperty("tomee.httpPort"));
        return url.toString();
    }
}
