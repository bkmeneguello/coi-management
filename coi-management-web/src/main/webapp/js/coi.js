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
		
		var AppView = Marionette.ItemView.extend({
			events: {
				'click #categorias': 'categorias'
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
				
				var categoria = this.model;
		      	_promptDelete(function() {
					categoria.destroy({
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
			events: {
				'click .adicionar': 'doCreate'
			},
			ui: {
				'produtos': 'table',
				'actions': 'footer'
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
			},
			onRender: function() {
				this.modelBinder().bind(this.model, this.el);
				this.$el.form();
				this.produtos.show(new CategoriaProdutosView({collection: this.model.get('produtos')}));
				this.comissoes.show(new CategoriaComissoesView({collection: this.model.get('comissoes')}));
			},
			doCancel: function(e) {
				e.preventDefault();
				Backbone.history.navigate('categorias', true);
			},
			doConfirm: function(e) {
				e.preventDefault();
				if (_validate(this)) {
					if (this.model.save()) {
						Backbone.history.navigate('categorias', true);
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
				'categoria(/:id)': 'updateCategoria'
			},
			index: function() {
				COI.body.show(new AppView());
			},
			categorias: function() {
				var categorias = new Categorias();
				categorias.fetch({
					success: function(categorias) {
						COI.body.show(new CategoriasView({collection: categorias}));
					}
				});
			},
			createCategoria: function() {
				var categoria = new Categoria();
				new Partes().fetch({
					success: function(partes) {
						var comissoes = [];
						partes.each(function(parte) {
							comissoes.push(new Comissao({
								parte: parte.get('descricao'),
								porcentagem: 0
							}));
						});
						categoria.get('comissoes').add(comissoes);
					}
				});
				COI.body.show(new CategoriaView({model: categoria}));
			},
			updateCategoria: function(id) {
				var categoria = new Categoria({id: id});
				categoria.fetch({
					success: function(categoria) {
						COI.body.show(new CategoriaView({model: categoria}));
					}
				});
			}
		});
		
		new Router();
		
		Backbone.history.start();
	});
})(jQuery);