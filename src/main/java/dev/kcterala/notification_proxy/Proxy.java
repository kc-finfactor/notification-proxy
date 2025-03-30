package dev.kcterala.notification_proxy;

import jakarta.persistence.*;

@Entity
public class Proxy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column
    private String team;

    @Column
    private String proxyUrl;

    @Column
    private String ttlForUrl;

    @Column
    private String subdomainToAdd;


    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(final String team) {
        this.team = team;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public void setProxyUrl(final String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    public String getTtlForUrl() {
        return ttlForUrl;
    }

    public void setTtlForUrl(final String ttlForUrl) {
        this.ttlForUrl = ttlForUrl;
    }

    public String getSubdomainToAdd() {
        return subdomainToAdd;
    }

    public void setSubdomainToAdd(final String subdomainToAdd) {
        this.subdomainToAdd = subdomainToAdd;
    }
}
