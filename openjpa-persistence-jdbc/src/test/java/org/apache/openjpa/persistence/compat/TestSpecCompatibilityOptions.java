/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.persistence.compat;

import javax.persistence.EntityManager;

import org.apache.openjpa.conf.Compatibility;
import org.apache.openjpa.conf.Specification;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.AbstractCachedEMFTestCase;

public class TestSpecCompatibilityOptions 
extends AbstractCachedEMFTestCase {
    
    /*
     * Verifies compatibility options and spec level are appropriate
     * for a version 2 persistence.xml
     */
    public void testJPA1CompatibilityOptions() {
        OpenJPAEntityManagerFactorySPI emf =
        (OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.
            createEntityManagerFactory("persistence_1_0",
                "org/apache/openjpa/persistence/compat/" +
                "persistence_1_0.xml");

        Compatibility compat = emf.getConfiguration().getCompatibilityInstance();
        assertTrue(compat.getFlushBeforeDetach());
        assertTrue(compat.getCopyOnDetach());
        assertTrue(compat.getPrivatePersistentProperties());
        String vMode = emf.getConfiguration().getValidationMode();
        assertEquals("NONE", vMode);
        Specification spec = emf.getConfiguration().getSpecificationInstance();
        assertEquals("JPA", spec.getName().toUpperCase());
        assertEquals(spec.getVersion(), 1);
        
        emf.close();

    }

    /*
     * Verifies compatibility options and spec level are appropriate
     * for a version 2 persistence.xml
     */
    public void testJPA2CompatibilityOptions() {
        OpenJPAEntityManagerFactorySPI emf =
        (OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.
            createEntityManagerFactory("persistence_2_0",
                "org/apache/openjpa/persistence/compat/" +
                "persistence_2_0.xml");

        Compatibility compat = emf.getConfiguration().getCompatibilityInstance();
        assertFalse(compat.getFlushBeforeDetach());
        assertFalse(compat.getCopyOnDetach());
        assertFalse(compat.getPrivatePersistentProperties());
        String vMode = emf.getConfiguration().getValidationMode();
        assertEquals("AUTO", vMode);
        Specification spec = emf.getConfiguration().getSpecificationInstance();
        assertEquals("JPA", spec.getName().toUpperCase());
        assertEquals(spec.getVersion(), 2);
        
        emf.close();
    }

    /*
     * Per JPA 2.0, Relationships in mapped superclass must be unidirectional.
     * An exceptioin will be thrown when a bi-directional relation is detected in
     * a mapped superclass. 
     */
    public void testMappedSuperClass() {
        OpenJPAEntityManagerFactorySPI emf =
            (OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.
                createEntityManagerFactory("persistence_2_0",
                    "org/apache/openjpa/persistence/compat/" +
                    "persistence_2_0.xml");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            EntityA a = new EntityA();
            a.setId(1);
            EntityB b = new EntityB();
            b.setId(1);
            a.setEntityB(b);
            b.setEntityA(a);
            em.getTransaction().begin();
            em.persist(a);
            em.persist(b);
            em.getTransaction().commit();
            em.close();
            fail("An exceptioin will be thrown for a bi-directional relation declared in mapped superclass");
        } catch (org.apache.openjpa.persistence.ArgumentException e) {
            if (em != null) {
                em.getTransaction().rollback();
                em.close();
            }
        } finally {
            emf.close();
        }
    }
}