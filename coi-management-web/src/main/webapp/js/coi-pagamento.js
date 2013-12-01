"use strict";

COI.module("Pagamento", function(Module, COI, Backbone, Marionette, $, _) {

	var Pagamento = Backbone.Model.extend({
		urlRoot: '/rest/pagamentos',
		defaults: {
			categoria: null,
			vencimento: null,
			descricao: null,
			valor: null,
			situacao: 'Pendente',
			pagamento: null,
			formaPagamento: null,
			banco: null,
			agencia: null,
			conta: null,
			cheque: null
		},
		parse: function(resp, options) {
			resp.vencimento = $.datepicker.parseDate('yy-mm-dd', resp.vencimento);
			resp.pagamento = resp.pagamento ? $.datepicker.parseDate('yy-mm-dd', resp.pagamento) : null;
			return resp;
		}
	});
	
	var Categoria = Backbone.Model.extend({
		urlRoot: '/rest/pagamentos/categorias',
		defaults: {
			descricao: null
		}
	});
	
	var Categorias = Backbone.Collection.extend({
		url: '/rest/pagamentos/categorias',
		model: Categoria
	});
	
	var CategoriaView = Marionette.Layout.extend({
		template: '#pagamento_categoria_item_template',
		templateHelpers: {
			elementos: []
		},
		ui: {
			'select': 'select'
		},
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		serializeData: function() {
			var data = Marionette.ItemView.prototype.serializeData.apply(this, Array.prototype.slice.apply(arguments));
			return _.extend(data, {
				name: this.options.attribute,
				label: this.options.label
			});
		},
		initialize: function() {
			var that = this;
			this.templateHelpers.elementos.length = 0;
			$.get('/rest/pagamentos/categorias', function(elementos) {
				$.each(elementos, function(index, element) {
					that.templateHelpers.elementos.push(element.descricao);
				});
				that.render();
			});
		},
		onRender: function() {
			this.modelBinder().bind(this.model, this.el);
			this.ui.select.input();
		},
	});
	
	var View = COI.FormView.extend({
		template: '#pagamento_template',
		regions: {
			'categoria': '#region-categoria'
		},
		ui: {
			'pagamento': '#pagamento',
			'cheque': '#cheque',
			'situacao': '#situacao',
			'formaPagamento': '#formaPagamento'
		},
		triggers: {
			'change #categoria': 'changeCategoria'
		},
		modelEvents: {
			'change:situacao': 'updateSituacao',
			'change:formaPagamento': 'updateFormaPagamento'
		},
		initialize: function() {
			if (!this.model.isNew()) {
				this.model.fetch();
			}
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['vencimento'].converter = dateConverter;
			bindings['valor'].converter = moneyConverter;
			bindings['pagamento'].converter = dateConverter;
			this.modelBinder().bind(this.model, this.el, bindings);

			this.updateSituacao(this.model, this.ui.situacao.val());
			this.updateFormaPagamento(this.model, this.ui.formaPagamento.val());
			this.renderCategoria();
		},
		onShow: function() {
			this.$el.find('input').first().focus();
		},
		renderCategoria: function() {
			this.categoria.show(new CategoriaView({model: this.model, label: 'Categoria:', attribute: 'categoria'}));
		},
		updateSituacao: function(model, value) {
			if ('Pago' == value) {
				this.ui.pagamento.show();
			} else {
				this.ui.pagamento.hide();
			}
		},
		updateFormaPagamento: function(model, value) {
			if ('Cheque' == value) {
				this.ui.cheque.show();
			} else {
				this.ui.cheque.hide();
			}
		},
		onCancel: function(e) {
			Backbone.history.navigate('pagamentos', true);
		},
		onConfirm: function(e) {
			if (_validate(this)) {
				this.model.save(null, {wait: true, success: this.onComplete, error: this.onError});
			}
		},
		onComplete: function(e) {
			Backbone.history.navigate('pagamentos', true);
			_notifySuccess();
		},
		onError: function(model, resp, options) {
			_notifyError(resp.responseText);
		}
	});
	
	var PagamentoCategoriasRowView = COI.ActionRowView.extend({
		template: '#pagamento_categoria_row_template',
		onUpdate: function(e) {
			Backbone.history.navigate('pagamento-categoria/' + this.model.get('id'), true);
		},
		onDelete: function(e) {
			_promptDelete(function() {
				e.model.destroy({
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
	
	var PagamentoCategoriasView = COI.GridView.extend({
		itemView: PagamentoCategoriasRowView,
		templateHelpers: {
			header: 'Categorias',
			columns: {
				descricao: 'Descrição'
			}
		},
		initialize: function() {
			this.collection.fetch();
		},
		onCancel: function(e) {
			Backbone.history.navigate('pagamentos', true);
		},
		onCreate: function(e) {
			Backbone.history.navigate('pagamento-categoria', true);
		}
	});
	
	var PagamentoCategoriaView = COI.FormView.extend({
		template: '#pagamento_categoria_template',
		initialize: function() {
			if (!this.model.isNew()) {
				this.model.fetch();
			}
		},
		onRender: function() {
			this.modelBinder().bind(this.model, this.el);
		},
		onShow: function() {
			this.$el.find('input').first().focus();
		},
		onCancel: function(e) {
			Backbone.history.navigate('pagamento-categorias', true);
		},
		onConfirm: function(e) {
			if (_validate(this)) {
				this.model.save(null, {wait: true, success: this.onComplete, error: this.onError});
			}
		},
		onComplete: function(e) {
			Backbone.history.navigate('pagamento-categorias', true);
			_notifySuccess();
		},
		onError: function(model, resp, options) {
			_notifyError(resp.responseText);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('pagamento', function() {
			COI.body.show(new View({model: new Pagamento()}));
		});
		COI.router.route('pagamento(/:id)', function(id) {
			COI.body.show(new View({model: new Pagamento({id: id})}));
		});
		COI.router.route('pagamento-categorias', function() {
			COI.body.show(new PagamentoCategoriasView({collection: new Categorias()}));
		});
		COI.router.route('pagamento-categoria', function() {
			COI.body.show(new PagamentoCategoriaView({model: new Categoria()}));
		});
		COI.router.route('pagamento-categoria(/:id)', function(id) {
			COI.body.show(new PagamentoCategoriaView({model: new Categoria({id: id})}));
		});
	});
});