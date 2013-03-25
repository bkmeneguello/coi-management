package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Categoria.CATEGORIA;
import static com.meneguello.coi.model.tables.Comissao.COMISSAO;
import static com.meneguello.coi.model.tables.Parte.PARTE;
import static com.meneguello.coi.model.tables.Produto.PRODUTO;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.Executor;
import com.meneguello.coi.model.tables.records.CategoriaRecord;
import com.meneguello.coi.model.tables.records.ParteRecord;
 
@Path("/categorias")
public class CategoriaEndpoint {
	
	@GET
	@Path("/new")
	@Produces(MediaType.APPLICATION_JSON)
	public Categoria createNew() throws Exception {
		final Categoria categoria = new Categoria();
		Result<ParteRecord> partesRecord = database().fetch(PARTE);
		for (ParteRecord parteRecord : partesRecord) {
			Comissao comissao = new Comissao();
			comissao.setParte(parteRecord.getDescricao());
			categoria.getComissoes().add(comissao);
		}
		return categoria;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Categoria> list() throws Exception {
		ArrayList<Categoria> categorias = new ArrayList<Categoria>();
		Result<CategoriaRecord> categoriasFromDB = database().fetch(CATEGORIA);
		for (CategoriaRecord categoriaFromDB : categoriasFromDB) {
			Categoria categoria = new Categoria();
			categoria.setDescricao(categoriaFromDB.getDescricao());
			categorias.add(categoria);
		}
		return categorias;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void create(Categoria categoria) throws Exception {
		Executor database = database();
		CategoriaRecord result = database.insertInto(CATEGORIA, CATEGORIA.DESCRICAO).values(categoria.getDescricao()).returning(CATEGORIA.ID).fetchOne();
		Long categoriaId = result.getId();
		for (Produto produto : categoria.getProdutos()) {
			database.insertInto(PRODUTO, PRODUTO.CATEGORIA_ID, PRODUTO.CODIGO, PRODUTO.DESCRICAO, PRODUTO.CUSTO, PRODUTO.PRECO)
			.values(categoriaId, produto.getCodigo(), produto.getDescricao(), produto.getCusto(), produto.getPreco())
			.execute();
		}
		for (Comissao comissao : categoria.getComissoes()) {
			ParteRecord parteRecord = database.selectFrom(PARTE).where(PARTE.DESCRICAO.eq(comissao.getParte())).fetchOne();
			database.insertInto(COMISSAO, COMISSAO.CATEGORIA_ID, COMISSAO.PARTE_ID, COMISSAO.PORCENTAGEM)
			.values(categoriaId, parteRecord.getId(), comissao.getPorcentagem())
			.execute();
		}
	}
 
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Categoria get(@PathParam("id") Long id) throws Exception {
		CategoriaRecord categoriaFromDB = database().selectFrom(CATEGORIA).where(CATEGORIA.ID.eq(id)).fetchOne();
		Categoria categoria = new Categoria();
		categoria.setDescricao(categoriaFromDB.getDescricao());
		return categoria;
	}

	public Executor database() throws ClassNotFoundException, SQLException {
		return new Executor(openConnection(), SQLDialect.HSQLDB);
	}

	public Connection openConnection() throws ClassNotFoundException, SQLException {
		loadDriver();
		return DriverManager.getConnection(System.getProperty("database.url"), System.getProperty("database.username"), System.getProperty("database.password"));
	}

	public void loadDriver() throws ClassNotFoundException {
		Class.forName(System.getProperty("database.driver"));
	}
 
}

class Categoria {
	
	private String descricao;
	
	private List<Produto> produtos = new ArrayList<>();
	
	private List<Comissao> comissoes = new ArrayList<>();
	
	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public List<Produto> getProdutos() {
		return produtos;
	}

	public List<Comissao> getComissoes() {
		return comissoes;
	}
	
}

class Produto {
	
	private String codigo;
	
	private String descricao;
	
	private BigDecimal custo = BigDecimal.ZERO;
	
	private BigDecimal preco = BigDecimal.ZERO;

	public String getCodigo() {
		return codigo;
	}
	
	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}
	
	public String getDescricao() {
		return descricao;
	}
	
	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
	
	public BigDecimal getCusto() {
		return custo;
	}
	
	public void setCusto(BigDecimal custo) {
		this.custo = custo;
	}
	
	public BigDecimal getPreco() {
		return preco;
	}
	
	public void setPreco(BigDecimal preco) {
		this.preco = preco;
	}
	
}

class Comissao {
	
	private String parte;
	
	private BigDecimal porcentagem = BigDecimal.ZERO;

	public String getParte() {
		return parte;
	}

	public void setParte(String parte) {
		this.parte = parte;
	}

	public BigDecimal getPorcentagem() {
		return porcentagem;
	}

	public void setPorcentagem(BigDecimal porcentagem) {
		this.porcentagem = porcentagem;
	}
	
}