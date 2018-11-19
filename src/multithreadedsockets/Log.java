package multithreadedsockets;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author cnmoro
 */
@Entity
@Table(name = "log")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Log.findAll", query = "SELECT l FROM Log l")
    , @NamedQuery(name = "Log.findById", query = "SELECT l FROM Log l WHERE l.id = :id")
    , @NamedQuery(name = "Log.findByTemp", query = "SELECT l FROM Log l WHERE l.temp = :temp")
    , @NamedQuery(name = "Log.findByData", query = "SELECT l FROM Log l WHERE l.data = :data")})
public class Log implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @Column(name = "temp")
    private double temp;
    @Basic(optional = false)
    @Column(name = "umid")
    private double umid;
    @Basic(optional = false)
    @Column(name = "data")
    @Temporal(TemporalType.TIMESTAMP)
    private Date data;

    public Log() {
    }

    public Log(Integer id) {
        this.id = id;
    }

    public Log(Integer id, double temp, double umid, Date data) {
        this.id = id;
        this.temp = temp;
        this.umid = umid;
        this.data = data;
    }

    public Log(double temp, double umid, Date data) {
        this.temp = temp;
        this.umid = umid;
        this.data = data;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public double getTemp() {
        return temp;
    }

    public void setTemp(double temp) {
        this.temp = temp;
    }

    public double getUmid() {
        return umid;
    }

    public void setUmid(double umid) {
        this.umid = umid;
    }

    public Date getData() {
        return data;
    }

    public void setData(Date data) {
        this.data = data;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Log)) {
            return false;
        }
        Log other = (Log) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "multithreadedsockets.Log[ id=" + id + " ]";
    }

}
