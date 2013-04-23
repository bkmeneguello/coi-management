(function($) {
	$(function() {
		function _validate(element) {
			element.$(':coi-validate').validate('validate');
			if(element.$('.invalid').length) {
				_notifyValidation();
				return false;
			}
			return true;
		}
		
		function _notifyValidation() {
			noty({text: 'Verifique todos os campos', type: 'warning', timeout: 5000});
		}
		
		function _notifySuccess() {
			noty({text: 'Registro incluido com sucesso', type: 'success', timeout: 2000});
		}
		
		function _notifyDelete() {
			noty({text: 'Registro excluido com sucesso', type: 'success', timeout: 2000});
		}
		
		function _notifyDeleteFailure() {
			noty({text: 'Falha na exclusão do registro', type: 'error'});
		}
		
		function _promptDelete(confirm) {
			noty({
				text: 'Deseja realmente excluir o registro?',
				layout: 'center',
				buttons: [
				    {
					    addClass: 'btn',
					    text: 'Cancelar',
					    onClick: function($noty) {
					        $noty.close();
				    	}
				 	},
					{
						addClass: 'btn', 
						text: 'Confirmar', 
						onClick: function($noty) {
							confirm();
							$noty.close();					
						}
				    }
				],
				callback: {
					onShow: function() {
						$('button.btn').button();
					}
				}
			});
		}
		
		function autoNumericConverter(direction, value, attribute, model) {
			switch(direction) {
			case 'ModelToView':
				return model.get(attribute);
				break;
			case 'ViewToModel':
				return $(this.boundEls).autoNumeric('get');
				break;
			}
		};
		
		var COI = new Backbone.Marionette.Application();
		COI.addRegions({
			'body': 'body'
		});
		
		var Parte = Backbone.Model.extend({
			defaults: {
				descricao: null
			}
		});
		
		var PartesComissionadas = Backbone.Collection.extend({
			url: '/rest/partes/comissionadas',
			model: Parte
		});
		
		var Partes = Backbone.Collection.extend({
			url: '/rest/partes',
			model: Parte
		});
		
		var Produto = Backbone.Model.extend({
			defaults: {
				codigo: null,
				descricao: null,
				custo: 0,
				preco: 0
			}
		});
		
		var Produtos = Backbone.Collection.extend({
			model: Produto
		});
		
		var Comissao = Backbone.Model.extend({
			defaults: {
				parte: null,
				porcentagem: 0
			}
		});
		
		var Comissoes = Backbone.Collection.extend({
			model: Comissao
		});
		
		var Categoria = Backbone.Model.extend({
			urlRoot: '/rest/categorias',
			defaults: function() {
				return {
					descricao: null,
					produtos: new Produtos(),
					comissoes: new Comissoes()
				};
			},
			parse: function(resp, options) {
				resp.produtos = new Produtos(resp.produtos);
				resp.comissoes = new Comissoes(resp.comissoes);
				return resp;
			}
		});
		
		var Categorias = Backbone.Collection.extend({
			url: '/rest/categorias',
			model: Categoria
		});
		
		var Pessoa = Backbone.Model.extend({
			urlRoot: '/rest/pessoas',
			defaults: {
				nome: null,
				codigo: 0,
				partes: new Partes()
			},
			parse: function(resp, options) {
				resp.partes = new Partes(resp.partes);
				return resp;
			}
		});
		
		var Pessoas = Backbone.Collection.extend({
			url: '/rest/pessoas',
			model: Pessoa
		});
		
		var AppView = Marionette.ItemView.extend({
			events: {
				'click #categorias': 'categorias',
				'click #pessoas': 'pessoas'
			},
			template: '#index_template',
			initialize: function() {
				_.bindAll(this);
			},
			onRender: function() {
				this.$('button').button();
			},
			categorias: function(e) {
				Backbone.history.navigate('categorias', true);
			},
			pessoas: function(e) {
				Backbone.history.navigate('pessoas', true);
			}
		});
		
		var CategoriaRowView = Marionette.ItemView.extend({
			tagName: 'tr',
			template: '#categoria_row_template',
			events: {
				'click .update': 'doUpdate',
				'click .delete': 'doDelete'
			},
			initialize: function() {
				_.bindAll(this);
			},
			onRender: function() {
				this.$('button').button();
			},
			doUpdate: function(e) {
				e.preventDefault();
				Backbone.history.navigate('categoria/' + this.model.get('id'), true);
			},
			doDelete: function(e) {
				e.preventDefault();
				
				var model = this.model;
		      	_promptDelete(function() {
					model.destroy({
						wait: true,
						success: function(model, response, options) {
							_notifyDelete();
						},
						error: function(model, xhr, options) {
							_notifyDeleteFailure();
						}
					});
		      	});
			}
		});
		
		var CategoriasView = Marionette.CompositeView.extend({
			tagName: 'form',
			itemView: CategoriaRowView,
			itemViewContainer: 'tbody',
			template: '#categorias_template',
			events: {
				'click .cancel': 'doCancel',
				'click .create': 'doCreate'
			},
			ui: {
				'table': 'table'
			},
			initialize: function() {
				_.bindAll(this);
				this.collection.fetch();
			},
			onRender: function() {
				this.$el.form();
				this.ui.table.table().css('width', '100%');
			},
			doCancel: function(e) {
				e.preventDefault();
				Backbone.history.navigate('', true);
			},
			doCreate: function(e) {
				e.preventDefault();
				Backbone.history.navigate('categoria', true);
			}
		});
		
		var CategoriaProdutoView = Marionette.ItemView.extend({
			template: '#categoria_produto_template',
			modelBinder: function() {
				return new Backbone.ModelBinder();
			},
			initialize: function() {
				_.bindAll(this);
				this.original = this.model;
				this.model = this.model.clone();
			},
			onRender: function() {
				var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
				bindings['custo'].converter = autoNumericConverter;
				bindings['preco'].converter = autoNumericConverter;
				this.modelBinder().bind(this.model, this.el, bindings);
				
				var that = this;
				this.$el.form();
				this.$el.dialog({
					title: 'Produto',
					dialogClass: 'no-close',
					height: 450,
					width: 510,
					modal: true,
					buttons: {
						'Cancelar': that.onCancel,
						'Confirmar': that.onConfirm
					}
				});
			},
			onCancel: function(e) {
				e.preventDefault();
				this.$el.dialog('close');
				this.close();
			},
			onConfirm: function(e) {
				e.preventDefault();
				if (_validate(this)) {
					this.original.set(this.model.attributes);
					this.collection.add(this.original);
					this.$el.dialog('close');
					this.close();
				}
			}
		});
		
		var CategoriaProdutosRowView = Marionette.ItemView.extend({
			tagName: 'tr',
			template: '#categoria_produtos_row_template',
			events: {
				'click .update': 'onUpdate',
				'click .delete': 'onDelete'
			},
			initialize: function() {
				_.bindAll(this);
				this.listenTo(this.model, 'change', this.render);
			},
			onRender: function() {
				this.$el.form();
			},
			onUpdate: function(e) {
				e.preventDefault();
				new CategoriaProdutoView({model: this.model, collection: this.model.collection}).render(); //FIXME
			},
			onDelete: function(e) {
				e.preventDefault();
				this.model.collection.remove(this.model);
			}
		});
		
		var CategoriaProdutosView = Marionette.CompositeView.extend({
			template: '#categoria_produtos_template',
			itemView: CategoriaProdutosRowView,
			itemViewContainer: 'tbody',
			ui: {
				'produtos': 'table'
			},
			initialize: function() {
				_.bindAll(this);
			},
			onRender: function() {
				this.$el.form();
				this.ui.produtos.table({
					buttons: {
						'Adicionar': this.doCreate
					}
				}).css('width', '100%');
			},
			doCreate: function(e) {
				e.preventDefault();
				new CategoriaProdutoView({model: new Produto(), collection: this.collection}).render(); //FIXME
			}
		});
		
		var CategoriaComissaoView = Marionette.ItemView.extend({
			tagName: 'tr',
			template: '#categoria_comissao_template',
			modelBinder: function() {
				return new Backbone.ModelBinder();
			},
			initialize: function() {
				_.bindAll(this);
			},
			onRender: function() {
				var bindings = Backbone.ModelBinder.createDefaultBindings(this.$el, 'name');
				bindings['porcentagem'].converter = autoNumericConverter;
				this.modelBinder().bind(this.model, this.$el, bindings);
				this.$el.form();
			}
		});
		
		var CategoriaComissoesView = Marionette.CompositeView.extend({
			template: '#categoria_comissoes_template',
			itemView: CategoriaComissaoView,
			itemViewContainer: 'table',
			initialize: function() {
				_.bindAll(this);
			},
			onRender: function() {
				this.$el.form();
			}
		});
		
		var CategoriaView = Marionette.Layout.extend({
			tagName: 'form',
			template: '#categoria_template',
			modelBinder: function() {
				return new Backbone.ModelBinder();
			},
			events: {
				'click .confirmar': 'doConfirm',
				'click .cancelar': 'doCancel'
			},
			regions: {
				'produtos': '#produtos',
				'comissoes': '#comissoes'
			},
			initialize: function() {
				_.bindAll(this);
				this.listenTo(this.model, 'change:produtos', this.renderProdutos);
				this.listenTo(this.model, 'change:comissoes', this.renderComissoes);
				if (this.model.isNew()) {
					this.onNew();
				} else {
					this.model.fetch();
				}
			},
			onRender: function() {
				this.modelBinder().bind(this.model, this.el);
				this.$el.form();
				this.renderProdutos();
				this.renderComissoes();
			},
			renderProdutos: function() {
				this.produtos.show(new CategoriaProdutosView({collection: this.model.get('produtos')}));
			},
			renderComissoes: function() {
				this.comissoes.show(new CategoriaComissoesView({collection: this.model.get('comissoes')}));
			},
			onNew: function() {
				var model = this.model;
				new PartesComissionadas().fetch({
					success: function(partes) {
						var comissoes = [];
						partes.each(function(parte) {
							comissoes.push(new Comissao({
								parte: parte.get('descricao'),
								porcentagem: 0
							}));
						});
						model.get('comissoes').add(comissoes);
					}
				});
			},
			doCancel: function(e) {
				e.preventDefault();
				Backbone.history.navigate('categorias', true);
			},
			doConfirm: function(e) {
				e.preventDefault();
				if (_validate(this)) {
					if (this.model.save(null, {wait: true})) {
						Backbone.history.navigate('categorias', true);
						_notifySuccess();
					}
				}
			}
		});
		
		var PessoaRowView = Marionette.ItemView.extend({
			tagName: 'tr',
			template: '#pessoa_row_template',
			events: {
				'click .update': 'doUpdate',
				'click .delete': 'doDelete'
			},
			initialize: function() {
				_.bindAll(this);
			},
			onRender: function() {
				this.$('button').button();
			},
			doUpdate: function(e) {
				e.preventDefault();
				Backbone.history.navigate('pessoa/' + this.model.get('id'), true);
			},
			doDelete: function(e) {
				e.preventDefault();
				
				var model = this.model;
		      	_promptDelete(function() {
					model.destroy({
						wait: true,
						success: function(model, response, options) {
							_notifyDelete();
						},
						error: function(model, xhr, options) {
							_notifyDeleteFailure();
						}
					});
		      	});
			}
		});
		
		var PessoasView = Marionette.CompositeView.extend({
			tagName: 'form',
			itemView: PessoaRowView,
			itemViewContainer: 'tbody',
			template: '#pessoas_template',
			events: {
				'click .cancel': 'doCancel',
				'click .create': 'doCreate'
			},
			ui: {
				'table': 'table'
			},
			initialize: function() {
				_.bindAll(this);
				this.collection.fetch();
			},
			onRender: function() {
				this.$el.form();
				this.ui.table.table().css('width', '100%');
			},
			doCancel: function(e) {
				e.preventDefault();
				Backbone.history.navigate('', true);
			},
			doCreate: function(e) {
				e.preventDefault();
				Backbone.history.navigate('pessoa', true);
			}
		});
		
		var PessoaParteView = Marionette.ItemView.extend({
			tagName: 'dd',
			template: '#pessoa_parte_template',
			modelBinder: function() {
				return new Backbone.ModelBinder();
			},
			initialize: function() {
				_.bindAll(this);
			},
			onRender: function() {
				this.modelBinder().bind(this.model, this.$el);
				this.$el.form();
			}
		});
		
		var PessoaPartesView = Marionette.CompositeView.extend({
			template: '#pessoa_partes_template',
			itemView: PessoaParteView,
			itemViewContainer: 'dl',
			templateHelpers: {
				partes_list: []
			},
			initialize: function() {
				_.bindAll(this);
				var that = this;
				new Partes().fetch({
					success: function(partes) {
						partes.each(function(parte) {
							that.templateHelpers.partes_list.push(parte.get('descricao'));
						});
						that.render();
					}
				});
			},
			onRender: function() {
				this.$el.form();
			}
		});
		
		var PessoaView = Marionette.Layout.extend({
			tagName: 'form',
			template: '#pessoa_template',
			modelBinder: function() {
				return new Backbone.ModelBinder();
			},
			events: {
				'click .confirmar': 'doConfirm',
				'click .cancelar': 'doCancel'
			},
			regions: {
				'partes': '#partes'
			},
			initialize: function() {
				_.bindAll(this);
				this.listenTo(this.model, 'change:partes', this.renderPartes);
				if (this.model.isNew()) {
					this.onNew();
				} else {
					this.model.fetch();
				}
			},
			onRender: function() {
				this.modelBinder().bind(this.model, this.el);
				this.$el.form();
				this.renderPartes();
			},
			renderPartes: function() {
				this.partes.show(new PessoaPartesView({collection: this.model.get('partes')}));
			},
			onNew: function() {
				
			},
			doCancel: function(e) {
				e.preventDefault();
				Backbone.history.navigate('pessoas', true);
			},
			doConfirm: function(e) {
				e.preventDefault();
				if (_validate(this)) {
					if (this.model.save(null, {wait: true})) {
						Backbone.history.navigate('pessoas', true);
						_notifySuccess();
					}
				}
			}
		});
		
		var Router = Backbone.Router.extend({
			routes: {
				'': 'index',
				'categorias': 'categorias',
				'categoria': 'createCategoria',
				'categoria(/:id)': 'updateCategoria',
				'pessoas': 'pessoas',
				'pessoa': 'createPessoa',
				'pessoa(/:id)': 'updatePessoa'
			},
			index: function() {
				COI.body.show(new AppView());
			},
			categorias: function() {
				COI.body.show(new CategoriasView({collection: new Categorias()}));
			},
			createCategoria: function() {
				COI.body.show(new CategoriaView({model: new Categoria()}));
			},
			updateCategoria: function(id) {
				COI.body.show(new CategoriaView({model: new Categoria({id: id})}));
			},
			pessoas: function() {
				COI.body.show(new PessoasView({collection: new Pessoas()}));
			},
			createPessoa: function() {
				COI.body.show(new PessoaView({model: new Pessoa()}));
			},
			updatePessoa: function(id) {
				COI.body.show(new PessoaView({model: new Pessoa({id: id})}));
			}
		});
		
		new Router();
		
		Backbone.history.start();
	});
})(jQuery);