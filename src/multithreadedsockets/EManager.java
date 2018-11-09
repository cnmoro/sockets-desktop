package multithreadedsockets;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 * @author cnmoro
 */
public class EManager implements java.io.Serializable {

    static EntityManagerFactory emf;
    static EntityManager em;

    public static EntityManager getEntityManager() {
        if (emf == null || em == null) {
            emf = Persistence.createEntityManagerFactory("multiThreadedSocketsPU");
            em = emf.createEntityManager();

            return em;
        } else {
            return em;
        }
    }

}
